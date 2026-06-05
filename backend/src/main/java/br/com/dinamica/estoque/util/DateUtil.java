package br.com.dinamica.estoque.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateUtil {

	private DateUtil() {}

	public static LocalDateTime now() {
		return ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
	}

}
