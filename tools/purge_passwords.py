"""Firebase에 평문으로 저장된 비밀번호 필드를 삭제하는 일회성 스크립트.

대상
  - Realtime Database: user/<uid>/password
  - Firestore: 모든 컬렉션(하위 컬렉션 포함)의 문서에서 `pw`, `password` 필드

기본은 dry-run(조회만)이에요. 실제로 지우려면 --apply 를 붙이세요.
비밀번호 값은 화면에 출력하지 않고 개수만 보여줘요.

사용법은 tools/README.md 참고.
"""
import argparse
import sys
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, db, firestore

FIRESTORE_FIELDS = ("pw", "password")
RTDB_PATH = "user"
RTDB_FIELD = "password"
BATCH_SIZE = 400


def abort_if_key_in_repo(key_path: Path) -> None:
    """서비스 계정 키가 git 저장소 안에 있으면 중단한다 (실수로 커밋하는 것 방지)."""
    for parent in [key_path.resolve().parent, *key_path.resolve().parents]:
        if (parent / ".git").exists():
            sys.exit(f"중단: 키 파일이 git 저장소({parent}) 안에 있어요. 저장소 밖으로 옮기세요.")


def find_rtdb_targets() -> list[str]:
    users = db.reference(RTDB_PATH).get() or {}
    return [uid for uid, node in users.items() if isinstance(node, dict) and RTDB_FIELD in node]


def purge_rtdb(uids: list[str], apply: bool) -> None:
    print(f"[Realtime DB] {RTDB_PATH}/*/{RTDB_FIELD} 가 있는 계정: {len(uids)}개")
    if apply and uids:
        db.reference(RTDB_PATH).update({f"{uid}/{RTDB_FIELD}": None for uid in uids})
        print("[Realtime DB] 삭제 완료")


def walk_documents(collection):
    """컬렉션과 하위 컬렉션의 모든 문서를 돌려준다."""
    for doc in collection.stream():
        yield doc
        for sub in doc.reference.collections():
            yield from walk_documents(sub)


def find_firestore_targets(client) -> list:
    targets = []
    for collection in client.collections():
        for doc in walk_documents(collection):
            fields = [f for f in FIRESTORE_FIELDS if f in (doc.to_dict() or {})]
            if fields:
                targets.append((doc.reference, fields))
    return targets


def purge_firestore(client, targets: list, apply: bool) -> None:
    print(f"[Firestore] {'/'.join(FIRESTORE_FIELDS)} 필드가 있는 문서: {len(targets)}개")
    if not apply or not targets:
        return
    for start in range(0, len(targets), BATCH_SIZE):
        batch = client.batch()
        for ref, fields in targets[start:start + BATCH_SIZE]:
            batch.update(ref, {f: firestore.DELETE_FIELD for f in fields})
        batch.commit()
    print("[Firestore] 삭제 완료")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--key", required=True, type=Path, help="서비스 계정 키(JSON) 경로 (저장소 밖에 둘 것)")
    parser.add_argument("--db-url", required=True, help="Realtime Database URL")
    parser.add_argument("--apply", action="store_true", help="실제로 삭제한다 (없으면 dry-run)")
    args = parser.parse_args()

    abort_if_key_in_repo(args.key)
    firebase_admin.initialize_app(credentials.Certificate(str(args.key)), {"databaseURL": args.db_url})
    client = firestore.client()

    print("모드:", "삭제 (--apply)" if args.apply else "dry-run (조회만)")
    purge_rtdb(find_rtdb_targets(), args.apply)
    purge_firestore(client, find_firestore_targets(client), args.apply)

    if args.apply:
        print("\n검증: 다시 조회합니다.")
        left_rtdb = len(find_rtdb_targets())
        left_fs = len(find_firestore_targets(client))
        print(f"남은 항목  Realtime DB: {left_rtdb}개 / Firestore: {left_fs}개")
        sys.exit(0 if left_rtdb == 0 and left_fs == 0 else 1)


if __name__ == "__main__":
    main()
