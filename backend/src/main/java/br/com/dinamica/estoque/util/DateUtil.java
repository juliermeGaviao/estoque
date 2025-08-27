package br.com.dinamica.estoque.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public class DateUtil {

	private DateUtil() {}

	public static Date now() {
		ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));

		return Date.from(zonedDateTime.toInstant());
	}

}
