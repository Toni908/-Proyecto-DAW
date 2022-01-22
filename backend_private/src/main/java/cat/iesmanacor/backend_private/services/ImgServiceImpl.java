package cat.iesmanacor.backend_private.services;

import cat.iesmanacor.backend_private.entities.Img;
import cat.iesmanacor.backend_private.entities.Restaurant;
import cat.iesmanacor.backend_private.files.FileUploadUtil;
import cat.iesmanacor.backend_private.repositories.ImgRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ImgServiceImpl implements ImgService {
    @Autowired
    ImgRepository imgRepository;


    @Override
    public List<Img> findAllImgs() {
        return imgRepository.findAll();
    }

    @Override
    public Optional<Img> findImgById(BigInteger id) {
        return imgRepository.findById(id);
    }

    @Override
    public Img saveImg(Img imgNew) {
        if (imgNew!=null) {
            return imgRepository.save(imgNew);
        }
        return new Img();
    }

    @Override
    public void deleteImg(BigInteger id) {
        if (imgRepository.findById(id).isPresent()) {
            imgRepository.deleteById(id);
        }
    }

    @Override
    public void updateImg(Img img) {
        BigInteger id = img.getId_img();
        if (imgRepository.findById(id).isPresent()) {
            Img imgUpdate = new Img();
            imgUpdate.setId_img(img.getId_img());
            imgUpdate.setUrl(img.getUrl());
            imgUpdate.setRestaurant(img.getRestaurant());
            imgRepository.save(imgUpdate);
        }
    }

    @Override
    public List<Img> findImgByUrl(String url) {
        return imgRepository.findImgByUrl(url);
    }

    @Override
    public List<Img> findImgFromRestaurantByUseracount(BigInteger id) {
        return imgRepository.findImgFromRestaurantByUseracount(id);
    }

    @Override
    public List<Img> findImgFromRestaurantId(BigInteger id) {
        return imgRepository.findImgFromRestaurantId(id);
    }
}
