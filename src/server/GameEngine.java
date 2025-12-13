package server;

import common.Protocol;
import java.util.*;
import server.roles.*;

public class GameEngine {

    private GameRoom room;
    private Timer gameTimer;
    private GamePhase currentPhase = GamePhase.WAITING;

    // 타이머 시간 설정 (초)
    private static final int TIME_DISCUSSION = 60;
    private static final int TIME_VOTE = 30;
    private static final int TIME_NIGHT = 30;
    private static final int TIME_HUNTER = 15;

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

        // 시작 시 밤으로 설정
        room.setIsNight(true);
        room.broadcastMessage("--- [게임 시작] ---");
        room.broadcastMessage("[System] 직업이 배정되었습니다. 첫 번째 밤이 시작됩니다!");

        // 첫 번째 페이즈 시작 (밤)
        startPhase(GamePhase.NIGHT_ACTION);
    }

    // 페이지 전환 및 타이머 시작
    private void startPhase(GamePhase nextPhase) {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        this.currentPhase = nextPhase;
        int duration = 0;

        switch (nextPhase) {
            case DAY_DISCUSSION:
                room.incrementDay(); // 날짜 증가
                duration = TIME_DISCUSSION;
                room.setIsNight(false);
                room.broadcastMessage("[낮] " + room.getDayNumber() + "일차 아침이 밝았습니다. 토론 시간입니다. (" + duration + "초)");
                break;

            case DAY_VOTE:
                duration = TIME_VOTE;
                room.broadcastMessage("[투표] 투표 시간입니다. 처형할 사람을 선택하세요. (" + duration + "초)");
                break;

            case HUNTER_REVENGE:
                duration = TIME_HUNTER;
                room.setIsNight(false);
                room.broadcastMessage("[System] ☠사냥꾼이 사망했습니다! 15초 내에 저승 길동무를 선택합니다.");
                break;

            case NIGHT_ACTION:
                duration = TIME_NIGHT;
                room.setIsNight(true);
                room.broadcastMessage("[밤] 밤이 되었습니다. 능력을 사용하세요. (" + duration + "초)");
                break;
        }

        room.broadcastMessage(Protocol.CMD_PHASE + " " + nextPhase + " " + duration);

        gameTimer = new Timer();
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                nextPhase();
            }
        }, duration * 1000L);
    }

    // 다음 단계로 이동하는 로직
    private void nextPhase() {
        if (currentPhase == GamePhase.ENDED) {
            return;
        }

        switch (currentPhase) {
            case DAY_DISCUSSION:
                startPhase(GamePhase.DAY_VOTE);
                break;
            case DAY_VOTE:
                processVotingResult();
                break;
            case HUNTER_REVENGE:
                room.broadcastMessage("[System] 사냥꾼이 망설이다가 숨을 거두었습니다...");
                startPhase(GamePhase.NIGHT_ACTION);
                break;
            case NIGHT_ACTION:
                processNight();
                break;
        }
    }

    // 낮 투표 결과 처리
    private void processVotingResult() {
        room.broadcastMessage("[System] 투표 시간이 종료되었습니다. 개표를 진행합니다...");

        int result = room.processDayVoting();

        if (result == 1) {
            stopEngine();
        } else if (result == 2) {
            startPhase(GamePhase.HUNTER_REVENGE);
        } else {
            startPhase(GamePhase.NIGHT_ACTION);
        }
    }

    // 밤 결과 처리
    public void processNight() {
        Map<String, String> actions = room.getNightActions();
        String wolfTarget = actions.get("늑대인간");
        String guardTarget = actions.get("경비병");
        String seerTarget = actions.get("선견자");
        String dictatorTarget = actions.get("독재자");

        List<String> deathList = new ArrayList<>();
        List<String> nightMessages = new ArrayList<>(); // 메시지 버퍼

        // 0. 독재자 쿠데타 처리 (가장 먼저 처리해야 함)
        if (dictatorTarget != null) {
            ClientHandler targetClient = findClientByNickname(dictatorTarget);
            ClientHandler dictatorClient = findClientByRole("독재자"); // 헬퍼 메소드 필요

            if (targetClient != null && dictatorClient != null && !dictatorClient.isDead()) {
                nightMessages.add("========================================");
                nightMessages.add("[속보] 독재자 '" + dictatorClient.getNickname() + "' 님이 쿠데타를 일으켰습니다!");

                deathList.add(dictatorTarget);

                // 대상이 늑대인간(Mafia)인지 확인
                if ("Mafia".equals(targetClient.getRole().getFaction())) {
                    // 성공: 늑대인간 처형 + 독재자는 시장(Mayor) 등극
                    nightMessages.add("[성공] 처형된 '" + dictatorTarget + "' 님은 늑대인간이었습니다!");
                    nightMessages.add("[알림] '" + dictatorClient.getNickname() + "' 님이 도시의 영웅(시장)이 되었습니다.");
                } else {
                    // 실패: 독재자 본인 처형
                    nightMessages.add("[실패] '" + dictatorTarget + "' 님은 선량한 시민이었습니다.");
                    nightMessages.add("[처형] 무고한 시민을 건드린 독재자 '" + dictatorClient.getNickname() + "' 님이 대신 처형당합니다.");

                    // 독재자 사망 목록 추가
                    deathList.add(dictatorClient.getNickname());
                }
                nightMessages.add("========================================");
            }
        }

        // 1. 늑대/경비병 결과 계산
        if (wolfTarget != null) {
            if (wolfTarget.equals(guardTarget)) {
                nightMessages.add("[System] 간밤에 경비병이 누군가를 지켜냈습니다!");
            } else {
                deathList.add(wolfTarget);
            }
        } else {
            nightMessages.add("[System] 간밤에 아무 일도 일어나지 않았습니다.");
        }

        // 2. 선견자 로직 (개별 전송)
        if (seerTarget != null) {
            for (ClientHandler client : room.getClientsInRoom()) {
                if (client.getRoleName().equals("선견자")) {
                    ClientHandler targetClient = findClientByNickname(seerTarget);
                    if (targetClient != null) {
                        String result = targetClient.getRole().getFaction().equals("Mafia") ? "늑대(Mafia)" : "시민(Citizen)";
                        client.sendMessage("[선견자 능력] '" + seerTarget + "' 님은 [" + result + "] 입니다.");
                    }
                    break;
                }
            }
        }

        // 3. 낮 시작 알림을 먼저 보냄 
        startPhase(GamePhase.DAY_DISCUSSION);

        // 4. 저장해둔 밤 결과 메시지 출력
        for (String msg : nightMessages) {
            room.broadcastMessage(msg);
        }

        // 5. 사망자 처리
        boolean isGameEnded = false;
        boolean hunterDied = false;

        for (String deadUser : deathList) {
            room.broadcastMessage("[System] 비극적이게도 '" + deadUser + "' 님이 처참하게 살해당했습니다.");

            // 사냥꾼인지 확인
            ClientHandler victim = findClientByNickname(deadUser);
            if (victim != null && "사냥꾼".equals(victim.getRoleName())) {
                hunterDied = true;
            }

            if (room.killUser(deadUser, "NIGHT")) {
                isGameEnded = true;
            }
        }

        room.getNightActions().clear();

        if (isGameEnded) {
            stopEngine();
        } else if (hunterDied) {
            // 밤에 사냥꾼이 죽으면 즉시 복수 페이즈로 전환 (낮 타이머 취소됨)
            startPhase(GamePhase.HUNTER_REVENGE);
        }
    }

    // 역할 이름으로 생존한 클라이언트 찾기
    private ClientHandler findClientByRole(String roleName) {
        for (ClientHandler c : room.getClientsInRoom()) {
            if (!c.isDead() && roleName.equals(c.getRoleName())) {
                return c;
            }
        }
        return null;
    }

    public void triggerDictatorCoup(String dictatorName, String targetName) {
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }

        room.broadcastMessage("=============================================");
        room.broadcastMessage("[속보] 독재자 '" + dictatorName + "' 님이 쿠데타를 선포했습니다!");
        room.broadcastMessage("[속보] 투표가 즉시 중단되며, 독재자의 권한으로 '" + targetName + "' 님을 즉결 처형합니다.");
        room.broadcastMessage("=============================================");

        boolean gameEnded = room.killUser(targetName, "DICTATOR");

        if (!gameEnded) {
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    startPhase(GamePhase.NIGHT_ACTION);
                }
            }, 3000L);
        }
    }

    public void resumeAfterHunter() {
        if (currentPhase == GamePhase.HUNTER_REVENGE) {
            System.out.println("[GameEngine] 사냥꾼 발포 완료. 밤으로 전환합니다.");
            startPhase(GamePhase.NIGHT_ACTION);
        }
    }

    public void stopEngine() {
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
        this.currentPhase = GamePhase.ENDED;
        System.out.println("[GameEngine] 게임이 종료되어 엔진을 정지합니다.");
    }

    private ClientHandler findClientByNickname(String nickname) {
        for (ClientHandler c : room.getClientsInRoom()) {
            if (c.getNickname().equals(nickname)) {
                return c;
            }
        }
        return null;
    }

    private Role createRoleInstance(String roleName) {
        switch (roleName) {
            case "늑대인간":
                return new WolfRole();
            case "경비병":
                return new GuardRole();
            case "선견자":
                return new SeerRole();
            case "큐피드":
                return new CupidRole();
            case "사냥꾼":
                return new HunterRole();
            case "천사":
                return new AngelRole();
            case "독재자":
                return new DictatorRole();
            default:
                return new CitizenRole();
        }
    }
}
