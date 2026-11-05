package com.irionna.eternalreturninfo.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.irionna.eternalreturninfo.util.NicknameKey
import kotlinx.coroutines.tasks.await

/**
 * 회원탈퇴 시 사용자 데이터를 지운다. 같은 작업을 다시 실행해도 안전하게(멱등) 만들어서,
 * 중간에 실패하면 처음부터 다시 시도하면 된다. 계정(Auth)은 맨 마지막에 지운다.
 *
 *  - 내 게시글, 내가 쓴 댓글: 삭제. 단, 다른 사람의 신고가 접수된 것은 보관한다. (3개월, 방침 참고)
 *  - 내 채팅방 사본: 삭제. 상대방 쪽 사본은 그대로 둔다.
 *  - user 노드: 이름만 "탈퇴한 사용자"인 표시용 노드(withdrawn=true)로 바꾼다. (채팅 목록에서 대화가 사라지지 않게)
 *  - 가입자 문서와 닉네임 선점 문서: 삭제. (프로필 이미지는 앱에 내장된 실험체 이미지라서 따로 저장하지 않는다)
 */
class AccountDeleter(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val rtdb: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    class NeedsRecentLoginException : Exception("최근 로그인이 필요합니다")

    /** 어느 단계(또는 어느 경로)에서 실패했는지 알 수 있게 단계 이름을 붙여서 다시 던진다. */
    class StepFailedException(val step: String, cause: Throwable) : Exception("탈퇴 처리 실패: $step", cause)

    private suspend fun <T> step(name: String, block: suspend () -> T): T {
        try {
            return block()
        } catch (e: Exception) {
            Log.w(TAG, "실패한 단계: $name", e)
            throw StepFailedException(name, e)
        }
    }

    suspend fun delete(withdrawnName: String) {
        val user = auth.currentUser ?: return
        val uid = user.uid

        // Firebase 는 계정 삭제 전에 최근 로그인을 요구한다. 데이터부터 지우고 계정 삭제가 막히는 상황을 피한다.
        val lastSignIn = user.metadata?.lastSignInTimestamp ?: 0L
        if (nowMillis() - lastSignIn > RECENT_LOGIN_MILLIS) throw NeedsRecentLoginException()

        val root = rtdb.reference

        step("내 게시글 삭제") { deletePosts(uid) }
        step("내 댓글 삭제") { deleteComments(uid) }
        step("채팅방 사본 삭제") { deleteChatCopies(uid) }
        step("차단 목록 삭제") { root.child("blocks").child(uid).removeValue().await() }

        step("탈퇴 표시 노드 쓰기") {
            root.child("user").child(uid).setValue(
                mapOf("uid" to uid, "name" to withdrawnName, "withdrawn" to true)
            ).await()
        }

        step("가입자·닉네임 문서 삭제") { deleteFirestore(uid) }

        step("계정 삭제") { user.delete().await() }
    }

    /** 다른 사람의 신고가 있는 글/댓글은 보관한다. 본인이 자기 글을 신고한 것은 세지 않는다. */
    private fun hasReportByOthers(node: DataSnapshot, authorUid: String): Boolean =
        node.child("report").children.any { it.child("uid").getValue(String::class.java) != authorUid }

    private suspend fun deletePosts(uid: String) {
        val posts = rtdb.reference.child("post").orderByChild("author").equalTo(uid).get().await()
        for (post in posts.children) {
            if (hasReportByOthers(post, uid)) continue
            step("게시글 삭제 post/${post.key}") { post.ref.removeValue().await() }
        }
    }

    private suspend fun deleteComments(uid: String) {
        val posts = rtdb.reference.child("post").get().await()
        for (post in posts.children) {
            for (comment in post.child("comments").children) {
                if (comment.child("author").getValue(String::class.java) != uid) continue
                if (hasReportByOthers(comment, uid)) continue
                step("댓글 삭제 post/${post.key}/comments/${comment.key}") { comment.ref.removeValue().await() }
            }
        }
    }

    /** 방 이름은 "상대uid + 내uid" 가 내 사본이다. (앱의 senderRoom 과 같은 규칙) */
    private suspend fun deleteChatCopies(uid: String) {
        val users = rtdb.reference.child("user").get().await()
        for (other in users.children) {
            val otherUid = other.key ?: continue
            if (otherUid == uid) continue
            step("채팅방 사본 삭제 chats/${otherUid.take(6)}…") {
                rtdb.reference.child("chats").child(otherUid + uid).removeValue().await()
            }
        }
    }

    private suspend fun deleteFirestore(uid: String) {
        val userDoc = firestore.collection("EternalReturnInfo").document(uid)
        val nickname = userDoc.get().await().getString("nickName")
        val batch = firestore.batch()
        NicknameKey.normalize(nickname)?.let {
            batch.delete(firestore.collection("nicknames").document(it))
        }
        batch.delete(userDoc)
        batch.commit().await()
    }

    companion object {
        private const val TAG = "AccountDeleter"
        private const val RECENT_LOGIN_MILLIS = 4 * 60 * 1000L
    }
}
