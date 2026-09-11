package mari.samba.dto.fs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiskUsageDto {
    private String path;          // Путь, который мы проверяли
    private String total;         // Общий объем (напр. "50.0 GB")
    private String used;          // Занято
    private String available;     // Свободно
    private int usePercent;       // Процент заполненности
    private String mountPoint;    // Точка монтирования (напр. "/")
}