package uy.com.fulbito.service;

import uy.com.fulbito.dto.ImageDtos.UploadSignatureResponse;

public interface ImageStorage {
    UploadSignatureResponse prepare(String folder);
    StoredImage confirm(String folder, String publicId, long version, String signature);
    void delete(String publicId);

    record StoredImage(String publicId, String secureUrl) {}
}
