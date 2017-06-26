package com.mobile_me.imtv_player.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Created by pasha on 17.12.16.
 * глобальные настройки для проигрывания
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MTGlobalSetupRec implements Serializable {

    Long min_count_free; // Минимальное количество воспроизведений некоммерческого видео.
    Long count_days_before; // Число дней до начала воспроизведения видео, за которое необходимо выкачать файл.
    Long stats_send_time; // Число минут периода для отсылки файла статистики

    public Long getMin_count_free() {
        return min_count_free;
    }

    public void setMin_count_free(Long min_count_free) {
        this.min_count_free = min_count_free;
    }

    public Long getCount_days_before() {
        return count_days_before;
    }

    public void setCount_days_before(Long count_days_before) {
        this.count_days_before = count_days_before;
    }

    public Long getStats_send_time() {
        return stats_send_time;
    }

    public void setStats_send_time(Long stats_send_time) {
        this.stats_send_time = stats_send_time;
    }

    @Override
    public String toString() {
        return "MTGlobalSetupRec{" +
                "min_count_free=" + min_count_free +
                ", count_days_before=" + count_days_before +
                ", stats_send_time=" + stats_send_time +
                '}';
    }
}
