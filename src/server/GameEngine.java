package server;

import common.Protocol;
import java.util.*;
import server.roles.*;

public class GameEngine {

    private GameRoom room;
    private Timer gameTimer;
    private GamePhase currentPhase = GamePhase.WAITING;

    // 타이머 시간 설정 (초) todo 시간 조정 필요
    private static final int TIME_DISCUSSION = 60;  // 낮 토론 60초
    private static final int TIME_VOTE = 30;        // 투표 30초
    private static final int TIME_NIGHT = 30;       // 밤 30초
    private static final int TIME_HUNTER = 15;      // 사냥꾼 제한 시간 15초

    public GameEngine(GameRoom room) {
        this.room = room;
    }

    // 게임 시작
    public void assignRolesAndStartGame() {
        List<ClientHandler> clients = room.getClientsInRoom();
        String roleConfig = room.getCustomRoleConfig();

        String[] roleNames = roleConfig.split(",");

        if (roleNames.length != clients.size()) {
            room.broadcastMessage("[System] 오류: 역할 수(" + roleNames.length + ")와 플레이어 수(" + clients.size() + ")가 일치하지 않습니다.");
            return;
        }

        List<Role> rolesToAssign = new LinkedList<>();
        for (String roleName : roleNames) {
            Role role = createRoleInstance(roleName.trim());
            if (role != null) {
                rolesToAssign.add(role);
            } else {
                room.broadcastMessage("[System] 오류: 정의되지 않은 역할(" + roleName + ")이 포함되어 있습니다.");
                return;
            }
        }

        Collections.shuffle(rolesToAssign);

        for (int i = 0; i < clients.size(); i++) {
            ClientHandler client = clients.get(i);
            Role assignedRole = rolesToAssign.get(i);

            // 새 게임 시작 시 생존 상태 초기화
            client.setDead(false);

            client.setRole(assignedRole);
            client.sendMessage(
                    Protocol.CMD_ROLE_ASSIGN + " " + assignedRole.getName() + " " + assignedRole.getFaction()
            );
            System.out.println("[GameEngine] " + client.getNickname() + "에게 " + assignedRole.getName() + " 배정");
        }

        room.broadcastMessage(Protocol.CMD_GAME_ROLES + " " + roleConfig);

        room.setIsNight(false);
        room.broadcastMessage("--- [게임 시작] ---");
        room.broadcastMessage("[System] 직업이 배정되었습니다. 첫 번째 낮이 시작됩니다!");

        // 첫 번째 페이즈 시작 (타이머 가동)
        startPhase(GamePhase.DAY_DISCUSSION);
    }

    // 페이지 전환 및 타이머 시작
    private void startPhase(GamePhase nextPhase) {
        // 기존 타이머 취소
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        this.currentPhase = nextPhase;
        int duration = 0;
        String msg = "";

        switch (nextPhase) {
            case DAY_DISCUSSION:
                duration = TIME_DISCUSSION;
                msg = "[낮] 토론 시간입니다. (" + duration + "초)";
                room.setIsNight(false);
                break;
            case DAY_VOTE:
                duration = TIME_VOTE;
                msg = "[투표] 투표 시간입니다. 처형할 사람을 선택하세요. (" + duration + "초)";
                break;
            case HUNTER_REVENGE:
                duration = TIME_HUNTER;
                // 사냥꾼 페이즈는 '낮' 취급? 혹은 별도?
                // 채팅을 위해 setIsNight(false)로 두거나, ClientHandler에서 이 페이즈일 때 채팅 허용해야 함
                room.setIsNight(false);
                room.broadcastMessage("[System] ☠️ 사냥꾼이 사망했습니다! 15초 내에 저승 길동무를 선택합니다.");
                room.broadcastMessage("[System] 생존자들은 사냥꾼에게 최후의 변론을 할 수 있습니다.");
                break;
            case NIGHT_ACTION:
                duration = TIME_NIGHT;
                msg = "[밤] 밤이 되었습니다. 능력을 사용하세요. (" + duration + "초)";
                room.setIsNight(true);
                break;
        }

        // 클라이언트에게 상태 변경 알림 (/phase [phase] [time])
        room.broadcastMessage(Protocol.CMD_PHASE + " " + nextPhase + " " + duration);

        // 타이머 스케줄링
        gameTimer = new Timer();
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                nextPhase();    // 시간 종료 시 다음 단계로 이동
            }
        }, duration * 1000L);
    }

    // 다음 단계로 이동하는 로직 (순환 구조)
    private void nextPhase() {
        // 이미 게임이 끝난 상태면 아무것도 하지 않음
        if (currentPhase == GamePhase.ENDED) {
            return;
        }

        switch (currentPhase) {
            case DAY_DISCUSSION:
                startPhase(GamePhase.DAY_VOTE);
                break;
            case DAY_VOTE:
                processVotingResult();
                // 게임이 안 끝났으면 밤으로
                break;
            case HUNTER_REVENGE:
                room.broadcastMessage("[System] 사냥꾼이 망설이다가 숨을 거두었습니다...");
                startPhase(GamePhase.NIGHT_ACTION);
                break;
            case NIGHT_ACTION:
                processNight();
                // 게임이 안 끝났으면 낮으로
                if (currentPhase != GamePhase.ENDED) {
                    startPhase(GamePhase.DAY_DISCUSSION);
                }
                break;
        }
    }

    public void resumeAfterHunter() {
        if (currentPhase == GamePhase.HUNTER_REVENGE) {
            System.out.println("[GameEngine] 사냥꾼 발포 완료. 밤으로 전환합니다.");
            startPhase(GamePhase.NIGHT_ACTION);
        }
    }

    // 낮 투표 결과 처리
    private void processVotingResult() {
        room.broadcastMessage("[System] 투표 시간이 종료되었습니다.");
        int result = room.processDayVoting();

        if (result == 1) {
            stopEngine(); // 게임 종료
        } else if (result == 2) {
            // ★ 사냥꾼 발동 -> 사냥꾼 페이즈 시작
            startPhase(GamePhase.HUNTER_REVENGE);
        } else {
            // 아무 일 없음 or 일반 시민 사망 -> 밤으로 이동
            startPhase(GamePhase.NIGHT_ACTION);
        }
    }

    // 밤 결과 처리 (늑대, 경비병, 선견자)
    public void processNight() {
//        if (!room.isNight()) return;
//
//        room.setIsNight(false);
//        Map<String, String> nightActions = room.getNightActions();
//
//        // ... (밤 로직)
//
//        nightActions.clear();
//        room.broadcastMessage("[System] 이제 낮이 시작됩니다. 투표를 준비하세요.");
        Map<String, String> actions = room.getNightActions(); // { "늑대인간": "UserA", "경비병": "UserA", ... }

        String wolfTarget = actions.get("늑대인간");
        String guardTarget = actions.get("경비병");
        String seerTarget = actions.get("선견자");

        List<String> deathList = new ArrayList<>();

        // 1. 늑대 살해 로직
        if (wolfTarget != null) {
            if (wolfTarget.equals(guardTarget)) {
                room.broadcastMessage("[System] 간밤에 경비병이 누군가를 지켜냈습니다!");    // 경비병이 막음
            } else {
                deathList.add(wolfTarget); // 살해 성공
            }
        } else {
            room.broadcastMessage("[System] 간밤에 아무 일도 일어나지 않았습니다.");
        }

        // 2. 사망자 처리 및 승리 조건 확인
        boolean isGameEnded = false;

        for (String deadUser : deathList) {
            room.broadcastMessage("[System] 비극적이게도 '" + deadUser + "' 님이 처참하게 살해당했습니다.");
            if (room.killUser(deadUser)) {
                isGameEnded = true; // 게임 끝
            }
        }

        // 3. 선견자 로직 (개별 전송)
        if (seerTarget != null) {
            // 선견자 역할을 가진 ClientHandler 찾기
            for (ClientHandler client : room.getClientsInRoom()) {
                if (client.getRoleName().equals("선견자")) {
                    // 타겟의 직업 확인
                    ClientHandler targetClient = findClientByNickname(seerTarget);
                    if (targetClient != null) {
                        String result = targetClient.getRole().getFaction().equals("Mafia") ? "늑대(Mafia)" : "시민(Citizen)";
                        client.sendMessage("[선견자 능력] '" + seerTarget + "' 님은 [" + result + "] 입니다.");
                    }
                    break;
                }
            }
        }

        // 행동 기록 초기화
        room.getNightActions().clear();

        // 밤에 누군가 죽어서 게임이 끝났다면 엔진 정지
        if (isGameEnded) {
            stopEngine();
        }
    }

    // 엔진 정지 메소드
    protected void stopEngine() {
        if (gameTimer != null) {
            gameTimer.cancel(); // 타이머 취소
            gameTimer = null;
        }
        this.currentPhase = GamePhase.ENDED; // 상태를 '종료'로 변경
        System.out.println("[GameEngine] 게임이 종료되어 엔진을 정지합니다.");
    }

    // 닉네임으로 클라이언트 찾기 헬퍼
    private ClientHandler findClientByNickname(String nickname) {
        for (ClientHandler c : room.getClientsInRoom()) {
            if (c.getNickname().equals(nickname)) {
                return c;
            }
        }
        return null;
    }

    /**
     * 역할 이름에 따라 해당 Role 클래스의 인스턴스를 생성하는 헬퍼 메소드
     */
    private Role createRoleInstance(String roleName) {
        switch (roleName) {
            case "늑대인간":
                return new WolfRole(); // 내부에서 "늑대인간" 반환
            case "경비병":
                return new GuardRole(); // 내부에서 "경비병" 반환
            case "선견자":
                return new SeerRole(); // 내부에서 "선견자" 반환

            // 특수 직업들을 한글 이름으로 생성
            case "독재자":
                return new CitizenRole("독재자");
            case "마녀":
                return new CitizenRole("마녀");
            case "사냥꾼":
                return new HunterRole();
            case "천사":
                return new CitizenRole("천사");
            case "큐피드":
                return new CupidRole();
            case "시민":
                return new CitizenRole(); // 기본값 "시민"
            default:
                // 혹시 모를 예외 처리를 위해 기본 시민으로 반환하거나 null
                return new CitizenRole("시민");
        }
    }
}
