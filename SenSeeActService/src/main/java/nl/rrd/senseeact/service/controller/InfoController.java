package nl.rrd.senseeact.service.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.utils.AppComponents;
import io.swagger.v3.oas.annotations.Parameter;
import nl.rrd.senseeact.service.Configuration;
import nl.rrd.senseeact.service.exception.HttpException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;

@RestController
@RequestMapping("/v{version}/info")
public class InfoController {
	@RequestMapping(value="/urlQrCode", method=RequestMethod.GET)
	public void urlQrCode(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable("version")
			@Parameter(hidden=true)
			String versionName) throws HttpException, Exception {
		Configuration config = AppComponents.get(Configuration.class);
		String url = config.get(Configuration.BASE_URL) + "/";
		QRCodeWriter qrWriter = new QRCodeWriter();
		BitMatrix matrix = qrWriter.encode(url, BarcodeFormat.QR_CODE, 200,
				200);
		response.setHeader("Content-Type", "image/png");
		try (OutputStream out = response.getOutputStream()) {
			MatrixToImageWriter.writeToStream(matrix, "png", out);
		}
	}
}
