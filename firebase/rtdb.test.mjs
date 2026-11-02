import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { ref, get, set, update, remove, push } from 'firebase/database';
import fs from 'fs';
import assert from 'assert';

const A = 'A'.repeat(28), B = 'B'.repeat(28), C = 'C'.repeat(28);
const env = await initializeTestEnvironment({
  projectId: 'demo-er',
  database: { host: '127.0.0.1', port: 9000, rules: fs.readFileSync('database.rules.json', 'utf8') },
});
await env.clearDatabase();
const db = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).database();
let pass = 0, fail = 0;
async function t(name, fn) { try { await fn(); pass++; console.log('ok   -', name); } catch (e) { fail++; console.log('FAIL -', name, '\n      ', String(e.message).split('\n')[0]); } }

await env.withSecurityRulesDisabled(async (ctx) => {
  const d = ctx.database();
  await set(ref(d, 'user/' + A), { name: 'a', email: 'a@x.com', uid: A });
  await set(ref(d, 'post/p1'), { id: 'p1', author: A, title: 't', content: 'c', views: 0, date: 1 });
  await set(ref(d, 'post/p1/comments/c1'), { id: 'c1', author: B, content: 'hi', date: 1 });
  await set(ref(d, 'chats/' + A + B + '/messages/m1'), { message: 'hi' });
  await set(ref(d, 'seasonID'), 5);
});

// user
await t('비로그인은 user 를 못 읽는다', () => assertFails(get(ref(db(null), 'user'))));
await t('로그인 사용자는 user 전체를 읽는다', () => assertSucceeds(get(ref(db(B), 'user'))));
await t('본인 user 노드는 쓴다', () => assertSucceeds(set(ref(db(B), 'user/' + B), { name: 'b', uid: B })));
await t('남의 user 노드는 못 쓴다', () => assertFails(set(ref(db(B), 'user/' + A + '/name'), 'hacked')));
await t('남의 user 노드는 못 지운다', () => assertFails(remove(ref(db(B), 'user/' + A))));

// post
await t('비로그인은 post 를 못 읽는다', () => assertFails(get(ref(db(null), 'post'))));
await t('로그인 사용자는 post 를 읽는다', () => assertSucceeds(get(ref(db(B), 'post'))));
await t('본인 uid 를 author 로 글을 쓴다', () => assertSucceeds(set(ref(db(B), 'post/p2'), { id: 'p2', author: B, title: 't', content: 'c', views: 0, date: 1 })));
await t('남의 uid 를 author 로 글을 못 쓴다', () => assertFails(set(ref(db(B), 'post/p3'), { id: 'p3', author: A, title: 't', content: 'c', views: 0, date: 1 })));
await t('작성자는 제목/내용을 수정한다', () => assertSucceeds(update(ref(db(A), 'post/p1'), { title: 'new', content: 'new2', category: '일상' })));
await t('작성자도 date 는 바꾸지 못한다', () => assertFails(update(ref(db(A), 'post/p1'), { date: 999 })));
await t('작성자가 댓글 목록을 통째로 덮어쓰지 못한다', () => assertFails(update(ref(db(A), 'post/p1'), { comments: {} })));
await t('남은 글을 수정하지 못한다', () => assertFails(update(ref(db(B), 'post/p1'), { title: 'x' })));
await t('남은 글을 지우지 못한다', () => assertFails(remove(ref(db(B), 'post/p1'))));
await t('작성자는 글을 지운다', () => assertSucceeds(remove(ref(db(B), 'post/p2'))));
await t('작성자 변경(탈취)은 못 한다', () => assertFails(update(ref(db(A), 'post/p1'), { author: B })));
await t('누구나 조회수를 올린다', () => assertSucceeds(set(ref(db(C), 'post/p1/views'), 1)));
await t('조회수에 문자열은 못 넣는다', () => assertFails(set(ref(db(C), 'post/p1/views'), 'abc')));
await t('댓글을 본인 이름으로 쓴다', () => assertSucceeds(set(ref(db(C), 'post/p1/comments/c2'), { id: 'c2', author: C, content: 'x', date: 1 })));
await t('남의 이름으로 댓글을 못 쓴다', () => assertFails(set(ref(db(C), 'post/p1/comments/c3'), { id: 'c3', author: A, content: 'x', date: 1 })));
await t('남의 댓글을 못 지운다', () => assertFails(remove(ref(db(C), 'post/p1/comments/c1'))));
await t('댓글 작성자는 지운다', () => assertSucceeds(remove(ref(db(B), 'post/p1/comments/c1'))));
await t('글 작성자도 남의 댓글은 따로 못 지운다', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => set(ref(ctx.database(), 'post/p1/comments/c4'), { id: 'c4', author: B, content: 'y', date: 1 }));
  await assertFails(remove(ref(db(A), 'post/p1/comments/c4')));
});
await t('게시글 신고는 본인 uid 로 한 건 추가', () => assertSucceeds(push(ref(db(C), 'post/p1/report'), { uid: C })));
await t('남의 uid 로 신고하지 못한다', () => assertFails(push(ref(db(C), 'post/p1/report'), { uid: A })));
await t('댓글 신고는 본인 uid 로', () => assertSucceeds(push(ref(db(C), 'post/p1/comments/c4/report'), { uid: C })));

// chats
const room1 = A + B, room2 = B + A;
await t('방 참여자(A)는 방을 읽는다', () => assertSucceeds(get(ref(db(A), 'chats/' + room1))));
await t('방 참여자(B)는 방을 읽는다', () => assertSucceeds(get(ref(db(B), 'chats/' + room1))));
await t('제3자(C)는 방을 못 읽는다', () => assertFails(get(ref(db(C), 'chats/' + room1))));
await t('제3자(C)는 방에 못 쓴다', () => assertFails(push(ref(db(C), 'chats/' + room1 + '/messages'), { message: 'x' })));
await t('참여자는 양쪽 방에 쓴다', async () => {
  await assertSucceeds(push(ref(db(A), 'chats/' + room1 + '/messages'), { message: 'x' }));
  await assertSucceeds(push(ref(db(A), 'chats/' + room2 + '/messages'), { message: 'x' }));
});
await t('chats 전체는 못 읽는다', () => assertFails(get(ref(db(A), 'chats'))));
await t('비로그인은 방을 못 읽는다', () => assertFails(get(ref(db(null), 'chats/' + room1))));

// seasonID / 기타
await t('seasonID 는 읽기만 된다', async () => {
  await assertSucceeds(get(ref(db(A), 'seasonID')));
  await assertFails(set(ref(db(A), 'seasonID'), 9));
});
await t('정의되지 않은 경로는 못 읽고 못 쓴다', async () => {
  await assertFails(get(ref(db(A), 'other')));
  await assertFails(set(ref(db(A), 'other'), 1));
});

console.log(`\n${pass} passed, ${fail} failed`);
await env.cleanup();
process.exit(fail ? 1 : 0);
