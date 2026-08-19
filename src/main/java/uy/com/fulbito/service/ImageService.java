package uy.com.fulbito.service;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.fulbito.domain.*;
import uy.com.fulbito.dto.ImageDtos.*;
import uy.com.fulbito.error.ApiException;
import uy.com.fulbito.repository.*;
import java.util.*;
@Service
public class ImageService {

    private final VenueImageRepository venueImages;
    private final CourtImageRepository courtImages;
    private final VenueService venues;
    private final CourtService courts;

    public ImageService(VenueImageRepository vi,CourtImageRepository ci,VenueService v,CourtService c){venueImages=vi;courtImages=ci;venues=v;courts=c;}

    @Transactional
    public ImageResponse addVenue(UUID venueId,AppUser owner,VenueImageRequest r){VenueImage i=new VenueImage();i.setVenue(venues.owned(venueId,owner));i.setUrl(r.url().trim());i.setStorageKey(clean(r.storageKey()));i.setSortOrder(r.sortOrder());i.setCover(r.cover());return response(venueImages.save(i));}

    @Transactional
    public ImageResponse addCourt(UUID courtId,AppUser owner,CourtImageRequest r){CourtImage i=new CourtImage();i.setCourt(courts.owned(courtId,owner));i.setUrl(r.url().trim());i.setStorageKey(clean(r.storageKey()));i.setSortOrder(r.sortOrder());return response(courtImages.save(i));}

    @Transactional(readOnly=true)
    public List<ImageResponse> venueList(UUID id){return venueImages.findByVenueIdOrderBySortOrder(id).stream().map(ImageService::response).toList();}

    @Transactional(readOnly=true)
    public List<ImageResponse> courtList(UUID id){return courtImages.findByCourtIdOrderBySortOrder(id).stream().map(ImageService::response).toList();}

    @Transactional
    public void deleteVenue(UUID id,AppUser owner){VenueImage i=venueImages.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Imagen no encontrada"));venues.owned(i.getVenue().getId(),owner);venueImages.delete(i);}

    @Transactional
    public void deleteCourt(UUID id,AppUser owner){CourtImage i=courtImages.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Imagen no encontrada"));courts.owned(i.getCourt().getId(),owner);courtImages.delete(i);}

    private static ImageResponse response(VenueImage i){
        return new ImageResponse(i.getId(),i.getUrl(),i.getStorageKey(),i.getSortOrder(),i.isCover());}

    private static ImageResponse response(CourtImage i){
        return new ImageResponse(i.getId(),i.getUrl(),i.getStorageKey(),i.getSortOrder(),false);}

    private String clean(String s){
        return s==null||s.isBlank()?null:s.trim();}
}
