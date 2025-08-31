package Utils;

import Start.StartIsHere;

public class StopIsHere {

    public static void stop() {
        try {
            Process p = StartIsHere.getStartedProcess();

            if (p == null || !p.isAlive()) {
                System.err.println("Процесс не найден или уже завершён.");
                return;
            }

            long pid = p.pid();
            String title = WindowUtils.getWindowTitleByPID(pid);

            if (title.isEmpty()) {
                System.err.println("Не удалось найти заголовок окна процесса.");
                return;
            }

            System.out.println("Найдено окно: " + title);

            if (WindowUtils.closeWindowByTitle(title)) {
                System.out.println("Сообщение WM_CLOSE отправлено.");

                boolean exited = p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                if (exited) {
                    System.out.println("Приложение завершилось корректно.");
                } else {
                    System.err.println("Приложение не закрылось в течение 5 секунд.");
                }
            } else {
                System.err.println("Не удалось отправить WM_CLOSE.");
            }

        } catch (Exception e) {
            System.err.println("Ошибка при закрытии приложения: " + e.getMessage());
        }
    }
}
