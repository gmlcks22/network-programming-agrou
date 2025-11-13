// Lobby.java
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// (참고: CopyOnWriteArrayList는 여러 스레드가 리스트를 읽고 수정할 때 
// 충돌을 방지(Thread-safe)하기 위해 사용합니다.)
public class Lobby {

    private List<GameRoom> gameRooms;

    public Lobby() {
        this.gameRooms = new CopyOnWriteArrayList<>();
        // 테스트를 위해 기본 방 1개 생성
        gameRooms.add(new GameRoom("기본방 (101호)"));
    }

    // 1. 방 생성 (ClientHandler가 호출)
    public synchronized void createRoom(String roomName, ClientHandler creator) {
        GameRoom newRoom = new GameRoom(roomName);
        gameRooms.add(newRoom);

        // 방을 생성한 사람은 자동으로 그 방에 참여
        newRoom.addClient(creator);
        creator.sendMessage("[System] " + roomName + " 방이 생성되었습니다. (자동 입장)");
    }

    // 2. 방 참여 (ClientHandler가 호출)
    public synchronized boolean joinRoom(String roomName, ClientHandler joiner) {
        for (GameRoom room : gameRooms) {
            if (room.getRoomName().equals(roomName)) {
                // (실제로는 인원수 체크 등이 필요)
                room.addClient(joiner);
                return true;
            }
        }
        return false; // 해당 이름의 방 없음
    }

    // (방 목록 보기, 방 제거 등의 기능 추가 가능)
}