package Processes;

import lombok.Getter;

@Getter
public class ProgressInfo {
    private final int dailyValue;
    private final int dailyTotal;
    private final long credit;
    private final int jade;

    public ProgressInfo(int dailyValue, int dailyTotal, long credit, int jade) {
        this.dailyValue = dailyValue;
        this.dailyTotal = dailyTotal;
        this.credit = credit;
        this.jade = jade;
    }

    @Override
    public String toString() {
        return  "- Ежедневки: " + dailyValue + "/" + dailyTotal + "\n" +
                "- Кредиты: " + credit + "\n" +
                "- Нефрит: " + jade;
    }
}
