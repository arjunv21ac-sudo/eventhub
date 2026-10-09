package com.eventhub.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

// Turns any text into a QR code PNG. Used by the QR endpoint and by confirmation emails.
@Service
public class QrCodeService {

    private static final int QR_SIZE = 300;

    public byte[] generatePng(String text) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException e) {
            throw new IllegalStateException("Could not generate QR code", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
