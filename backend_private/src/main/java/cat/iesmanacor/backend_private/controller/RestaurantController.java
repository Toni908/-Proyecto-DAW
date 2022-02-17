package cat.iesmanacor.backend_private.controller;

import cat.iesmanacor.backend_private.entities.*;
import cat.iesmanacor.backend_private.entityDTO.RestaurantDTO;
import cat.iesmanacor.backend_private.entityDTO.RestaurantSecureDTO;
import cat.iesmanacor.backend_private.entityDTO.SimpleUserDTO;
import cat.iesmanacor.backend_private.files.FileUploadUtil;
import cat.iesmanacor.backend_private.services.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static cat.iesmanacor.backend_private.componentes.Etiquetas.saveEtiquetas;
import static cat.iesmanacor.backend_private.componentes.User.getUser;
import static cat.iesmanacor.backend_private.componentes.User.isUserCorrect;


@Controller
public class RestaurantController {
    @Autowired
    RestaurantService restaurantService;

    @Autowired
    UseracountService useracountService;

    @Autowired
    MembresiaService membresiaService;

    @Autowired
    LocalidadService localidadService;

    @Autowired
    EtiquetasService etiquetasService;

    @Autowired
    CartaService cartaService;

    @Autowired
    MunicipioService municipioService;

    @Autowired
    Restaurante_EtiquetasService restaurante_etiquetasService;

    @Autowired
    ImgService imgService;

    @Autowired
    EmailService emailService;

    // LISTAS DE RESTURANTES POR X USUARIO

    @GetMapping("/lista/restaurantes")
    public String listRestaurants(ModelMap model, HttpServletRequest request){
        Useracount useracount = getUser(request);

        if (isUserCorrect(useracount,useracountService)) {
            ModelMapper modelMapper = new ModelMapper();
            SimpleUserDTO simpleUserDTO = modelMapper.map(useracount, SimpleUserDTO.class);
            model.addAttribute("cartas", getCartasRestaurantActive(useracount));
            model.addAttribute("restaurantesUser", restaurantService.findRestaurantByUseracount(useracount.getId_user()));
            model.addAttribute("ImgImages", imagesIsEmpties(useracount));
            model.addAttribute("user", simpleUserDTO);
            return "lista_restaurants";
        }
        return "redirect:/error/401";
    }

    public List<CartaIsEmpty> getCartasRestaurantActive(Useracount useracount) {
        List<Restaurant> restaurants = restaurantService.findRestaurantByUseracount(useracount.getId_user());
        List<CartaIsEmpty> cartaIsEmpties = new ArrayList<>();
        for (Restaurant restaurant : restaurants) {
            boolean hasVisible = false;
            if (!restaurant.getCartas().isEmpty()) {
                for (Carta carta : restaurant.getCartas()) {
                    if (carta.isVisible()) {
                        cartaIsEmpties.add(new CartaIsEmpty(restaurant,false,true));
                        hasVisible = true;
                        break;
                    }
                }
                if (!hasVisible) {
                    cartaIsEmpties.add(new CartaIsEmpty(restaurant,false,false));
                }
            } else {
                cartaIsEmpties.add(new CartaIsEmpty(restaurant,true,false));
            }
        }
        return cartaIsEmpties;
    }

    public List<ListImagesIsEmpty> imagesIsEmpties(Useracount useracount) {
        List<Restaurant> restaurants = restaurantService.findRestaurantByUseracount(useracount.getId_user());
        List<ListImagesIsEmpty> imagesIsEmpties = new ArrayList<>();
        for (Restaurant restaurant : restaurants) {
            List<Img> imgs = imgService.findImgFromRestaurantId(restaurant.getId_restaurante());
            if (imgs.isEmpty()) {
                imagesIsEmpties.add(new ListImagesIsEmpty(imgs,restaurant.getId_restaurante(), true));
            } else {
                imagesIsEmpties.add(new ListImagesIsEmpty(imgs,restaurant.getId_restaurante(),false));
            }
        }
        return imagesIsEmpties;
    }

    //////////////         RESTAURANTES   FORMULARIOS      ////////////////////

    @RequestMapping(value = "/restaurant/create", method = RequestMethod.GET)
    public String create(ModelMap model, HttpServletRequest request) {
        Useracount useracount = getUser(request);
        if (useracount!=null) {
            Optional<Useracount> useracountDDBB = useracountService.findUseracountById(useracount.getId_user());
            if (useracountDDBB.isPresent()) {
                if (useracountDDBB.get().equals(useracount)) {
                    model.addAttribute("restaurant", new RestaurantDTO());
                    model.addAttribute("etiqueta", new Etiquetas());
                    model.addAttribute("etiquetas", etiquetasService.findAllEtiquetas());
                    return "formularios/restaurante-create";
                }
            }
        }
        return "redirect:/error/401";
    }

    @RequestMapping(value = "/restaurant/update/{id}", method = RequestMethod.GET)
    public String update(@PathVariable BigInteger id, ModelMap model, HttpServletRequest request) {
        Useracount useracount = getUser(request);

        if (isUserCorrect(useracount,useracountService)) {
            Optional<Restaurant> restaurant = restaurantService.findRestaurantById(id);
            if (restaurant.isPresent()) {
                if (restaurant.get().getUseracount().equals(useracount)) {
                    ModelMapper modelMapper = new ModelMapper();
                    RestaurantSecureDTO restaurantDTO = modelMapper.map(restaurant.get(), RestaurantSecureDTO.class);
                    model.addAttribute("imageModified", getProvisionalNameImgFromUrlByUseracount(useracount));
                    model.addAttribute("imagesRestaurant", imgService.findImgFromRestaurantId(restaurant.get().getId_restaurante()));
                    model.addAttribute("restaurant", restaurantDTO);
                    model.addAttribute("etiqueta", new Etiquetas());
                    model.addAttribute("etiquetas", getEtiquetasFromRestaurant_Etiqueta(restaurant.get().getId_restaurante()));
                    model.addAttribute("array", localidadService.findAllLocalidad());
                    return "formularios/restaurante-update";
                }
            }
        }
        return "redirect:/error/401";
    }


    //////////////         RESTAURANTES   ACTIONS      ////////////////////

    @RequestMapping(value = "/restaurant/save")
    @Transactional()
    public String save(@ModelAttribute @Valid RestaurantDTO restaurantDTO,
                       BindingResult errors,
                       ModelMap model,
                       @RequestParam("image") MultipartFile multipartFile,
                       @RequestParam("etiquetas") List<String> etiquetas,
                       HttpServletRequest request) {
        inicializeModelMap(model);

        //Errores redirect
        if (errors.hasErrors()) {
            return "redirect:/error/401";
        }

        Useracount useracount = getUser(request);
        if (isUserCorrect(useracount,useracountService)) {
            ModelMapper modelMapper = new ModelMapper();
            Restaurant restaurant = modelMapper.map(restaurantDTO, Restaurant.class);

            restaurant.setUseracount(useracount);

            if (restaurant.getNombre()!=null) {
                if (!checkNameisEmpty(restaurant.getNombre())) {
                    Traductions traductions = new Traductions("El nombre del restaurante ya esta cogido!","Restaurant name already taken","El nom del restaurant ya esta en us");
                    model.addAttribute("error", traductions.getTraductionLocale(request));
                    return create(model,request);
                }
                if (restaurant.getLocalidad()!=null) {
                    if (restaurant.getLatitud()!=null || restaurant.getLongitud()!=null) {
                        // ELIMINAR PARA PRODUCION
                        restaurant.setValidated(true);

                        saveRestaurant(restaurant);
                        List<Restaurant> restaurantCreated = restaurantService.findRestaurantByNombre(restaurant.getNombre());
                        if (!restaurantCreated.isEmpty() && !etiquetas.isEmpty()) {
                            saveEtiquetas(etiquetas, restaurantCreated.get(0), etiquetasService, restaurante_etiquetasService);
                        }
                        saveImageRestaurantFirst(multipartFile, restaurant);
                        return "redirect:/restaurant/update/" + restaurant.getId_restaurante();
                    }
                    Traductions traductions = new Traductions("Localización no selecionado","Location not selected","Localització no seleccionat");
                    return create(model.addAttribute("error", traductions.getTraductionLocale(request)),request);
                }
            }
            Traductions traductions = new Traductions("Nombre no disponible","Name dont available","Nom no disponible");
            return create(model.addAttribute("error", traductions.getTraductionLocale(request)),request);
        }
        return "redirect:/error/401";
    }

    @RequestMapping(value = "/restaurant/put")
    public String put(@ModelAttribute @Valid RestaurantDTO restaurantDTO, @RequestParam("myLocalidadChanged") String myLocalidadChanged, BindingResult errors, ModelMap model, HttpServletRequest request) {
        inicializeModelMap(model);
        Useracount useracount = getUser(request);

        if (isUserCorrect(useracount,useracountService)) {
            if (errors.hasErrors()) {
                return "redirect:/error/401";
            }
            ModelMapper modelMapper = new ModelMapper();
            Restaurant restaurant = modelMapper.map(restaurantDTO, Restaurant.class);

            if (restaurant.getId_restaurante() != null) {
                Optional<Restaurant> restaurantBefore = restaurantService.findRestaurantById(restaurant.getId_restaurante());
                if (restaurantBefore.isPresent()) {
                    if (!localidadService.findLocalidadByNombre_localidad(myLocalidadChanged).isEmpty()) {
                        restaurant.setLocalidad(localidadService.findLocalidadByNombre_localidad(myLocalidadChanged).get(0));
                    } else {
                        restaurant.setLocalidad(restaurantBefore.get().getLocalidad());
                    }
                    model = defaultValuesRestaurant(restaurant, model, request, restaurantBefore);
                } else {
                    return "redirect:/error/401";
                }
                return update(restaurant.getId_restaurante(), model, request);
            }
        }
        return "redirect:/error/401";
    }

    private ModelMap defaultValuesRestaurant(@ModelAttribute @Valid Restaurant restaurant, ModelMap model, HttpServletRequest request, Optional<Restaurant> restaurantBefore) {
        if (restaurantBefore.isPresent()) {
            restaurant.setMembresia(restaurantBefore.get().getMembresia());
            restaurant.setUseracount(restaurantBefore.get().getUseracount());
            restaurant.setCartas(restaurantBefore.get().getCartas());
            restaurant.setVisible(restaurantBefore.get().isVisible());
            restaurant.setValidated(restaurantBefore.get().isValidated());
            model = checkToUpdate(restaurant, restaurantBefore.get(), model, request);
            Traductions traductions = new Traductions("Cambios realizados correctamente", "Changes made successfully", "Canvis realitzats correctament");
            model.addAttribute("success", traductions.getTraductionLocale(request));
        } else{
            Traductions traductions = new Traductions("No se pudo realizar el cambio", "Changes cant be done", "No es pot fer els cambis");
            model.addAttribute("success", traductions.getTraductionLocale(request));
        }
        return model;
    }

    @RequestMapping(value = "/restaurant/visibility", method = RequestMethod.POST, produces = "application/json")
    public String visibility(@RequestParam("idRestaurante") BigInteger id,@RequestParam(name = "visibilty",defaultValue = "false") boolean visibilidad, ModelMap model,HttpServletRequest request) {
        Useracount useracount = getUser(request);
        if (isUserCorrect(useracount,useracountService)) {
            if (id != null) {
                Optional<Restaurant> restaurant = restaurantService.findRestaurantById(id);
                if (restaurant.isPresent() && restaurant.get().getUseracount().equals(useracount)) {
                    if (visibilidad) {
                        List<Img> imgs = imgService.findImgFromRestaurantId(restaurant.get().getId_restaurante());
                        if (!imgs.isEmpty()) {
                            restaurant.get().setVisible(true);
                            updateRestaurant(restaurant.get());
                            Traductions traductions = new Traductions("El restaurante " + restaurant.get().getNombre() + " es visible","Restaurant "+restaurant.get().getNombre()+" is visible","El Restaurant "+restaurant.get().getNombre()+" es visible");
                            model.addAttribute("success", traductions.getTraductionLocale(request));
                        } else {
                            Traductions traductions = new Traductions("El restaurante no tiene imagen, no se puede hacer visible","The restaurant has no image, it cannot be made visible","El restaurant no té imatge, no es pot fer visible");
                            model.addAttribute("error", traductions.getTraductionLocale(request));
                        }
                    } else {
                        restaurant.get().setVisible(false);
                        updateRestaurant(restaurant.get());
                        Traductions traductions = new Traductions("El restaurante " + restaurant.get().getNombre() + " es invisible","Restaurant "+restaurant.get().getNombre()+" is invisible","El Restaurant "+restaurant.get().getNombre()+" es invisible");
                        model.addAttribute("success", traductions.getTraductionLocale(request));
                    }
                    return update(restaurant.get().getId_restaurante(), model, request);
                } else {
                    return "redirect:/error/401";
                }
            }
        }
        return "redirect:/error/401";
    }

    @RequestMapping(value = "/restaurant/validation", method = RequestMethod.POST, produces = "application/json")
    public String validation(@RequestParam("idRestaurant") BigInteger id,@RequestParam(name = "validationResponse",defaultValue = "false") boolean validation, HttpServletRequest request) {
        // ONLY ADMINS
        Useracount useracount = getUser(request);

        if (isUserCorrect(useracount,useracountService)) {
            if (id != null) {
                if (useracount.isAdmin()) {
                    Optional<Restaurant> restaurant = restaurantService.findRestaurantById(id);

                    if (restaurant.isPresent()) {
                        restaurant.get().setValidated(validation);
                        updateRestaurant(restaurant.get());
                        if (validation) {
                            if (useracount.getCorreo()!=null) {
                                emailService.sendSimpleMessage(useracount.getCorreo(), "Validacion " + restaurant.get().getNombre(), "El restaurante " + restaurant.get().getNombre() + " acaba de ser validado por un administrador, ahora mismo ya puede ser visible para todos los usuarios, para realizar algun cambio por si aun no lo has hecho http://restaurantemallorca.me:8080/restaurant/update/" + restaurant.get().getId_restaurante() + " , para mas info visite a la pestaña de preguntas.");
                            }
                        }
                    }
                    return "redirect:/restaurante/configuration/admin";
                } else {
                    return "redirect:/error/401";
                }
            }
        }
        return "redirect:/error/401";
    }

    // ADMIN SECTION DATATABLE RESTAURANTES
    @GetMapping("/restaurante/configuration/admin")
    public String adminRestaurantes(ModelMap model, HttpServletRequest request){
        Useracount useracount = getUser(request);

        if (isUserCorrect(useracount, useracountService)) {
            if (useracount.isAdmin()) {
                model.addAttribute("restaurantes", restaurantService.findAllRestaurants());
                model.addAttribute("updateRestaurant", new Restaurant());
                return "restaurantes_admin";
            }
        }
        return "redirect:/error/401";
    }

    /* ------------------------------------------ */

    public void saveRestaurant(Restaurant restaurant) {
        restaurantService.saveRestaurant(restaurant);
    }

    @RequestMapping(value = "/restaurant/delete/{id}", method = RequestMethod.GET)
    public RedirectView delete(@PathVariable BigInteger id, HttpServletRequest request) {
        Useracount useracount = getUser(request);

        if (isUserCorrect(useracount, useracountService)) {
            if (id != null) {
                Optional<Restaurant> restaurant = restaurantService.findRestaurantById(id);
                if (restaurant.isPresent()) {
                    if (restaurant.get().getUseracount().equals(useracount)) {
                        List<Img> imgs = imgService.findImgFromRestaurantId(restaurant.get().getId_restaurante());
                        // DELETE IMG BEFORE DELETE ALL
                        for (Img singleId : imgs) {
                            Optional<Img> imgSelected = imgService.findImgById(singleId.getId_img());
                            if (imgSelected.isPresent()) {
                                imgService.deleteImg(imgSelected.get().getId_img());
                                String uploadDir = "" + imgSelected.get().getRestaurant().getId_restaurante();
                                FileUploadUtil.deleteImg(uploadDir, imgSelected.get().getUrl());
                            }
                        }
                        restaurantService.deleteRestaurant(id);
                    }
                }
            }
        }
        return new RedirectView("/lista/restaurantes");
    }

    public void updateRestaurant(Restaurant restaurantNew) {
        restaurantService.updateRestaurant(restaurantNew);
    }

    public void inicializeModelMap(ModelMap model) {
        model.remove("restaurant");
        model.remove("restaurants");
        model.remove("error");
    }

    public boolean checkNameisEmpty(String name) {
        return restaurantService.findRestaurantByNombre(name).isEmpty();
    }

    public boolean checkMembresiaisEmpty(BigInteger id) {
        return restaurantService.findRestaurantById_Membresia(id).isEmpty();
    }

    public boolean isNameRestaurantTaken(Restaurant restaurant, Restaurant restaurantBefore) {
        if (restaurant.getNombre()!=null) {
            if (restaurant.getNombre().equals(restaurantBefore.getNombre())) {
                return false;
            } else return !checkNameisEmpty(restaurant.getNombre());
        }
        return true;
    }

    public ModelMap checkToUpdate(Restaurant restaurant, Restaurant restaurantBefore, ModelMap model, HttpServletRequest request) {
        if (isNameRestaurantTaken(restaurant,restaurantBefore)) {
            Traductions traductions = new Traductions("Nombre ya esta cogido","Name already taken","Nom en us");
            model.addAttribute("error",traductions.getTraductionLocale(request));
            return model;
        }
        updateRestaurant(restaurant);
        return model;
    }


    // ETIQUETA RESTAURANT RELATION

    public boolean checkNameEtiquetasIsEmpty(Etiquetas etiquetas) {
        if (etiquetas.getNombre()!=null) {
            return etiquetasService.findEtiquetaByName(etiquetas.getNombre()).isEmpty();
        }
        return false;
    }

    public List<Etiquetas> getEtiquetasFromRestaurant_Etiqueta(BigInteger id) {
        List<Restaurante_Etiquetas> restaurante_etiquetas = restaurante_etiquetasService.getRestaurant_EtiquetasFromIdRestaurant(id);
        List<Etiquetas> etiquetas = new ArrayList<>();
        for (Restaurante_Etiquetas restaurante_etiqueta : restaurante_etiquetas) {
            Optional<Etiquetas> etiqueta = etiquetasService.findEtiquetaById(restaurante_etiqueta.getId().getEtiquetas().getId_etiqueta());
            etiqueta.ifPresent(etiquetas::add);
        }
        return etiquetas;
    }

    // IMG

    public void saveImageRestaurantFirst(MultipartFile multipartFile, Restaurant restaurant) {
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
        Img img = new Img();
        img.setRestaurant(restaurant);
        img.setUrl(fileName);
        try {
            Img imgSumbited = imgService.saveImg(img);
            fileName = imgSumbited.getId_img()+fileName;
            imgSumbited.setUrl(fileName);
            imgService.updateImg(imgSumbited);
            String uploadDir = "restaurantes-photos/"+img.getRestaurant().getId_restaurante();
            FileUploadUtil.saveFile(uploadDir, fileName, multipartFile);

        } catch (Exception e) {
            //
        }
    }

    public List<ArrayList<String>> getProvisionalNameImgFromUrlByUseracount(Useracount useracount) {
        List<Img> imgs = imgService.findImgFromRestaurantByUseracount(useracount.getId_user());
        List<ArrayList<String>> provisional = new ArrayList<>();
        for (Img img : imgs) {
            ArrayList<String> array = new ArrayList<>();
            array.add(0,img.getUrl());
            String urlModified = deleteCharsFromString(img.getUrl(), img.getId_img());
            array.add(1,urlModified);
            provisional.add(array);
        }
        return provisional;
    }

    public String deleteCharsFromString(String toUpdate, BigInteger number) {
        int numberInt = number.toString().length();
        return toUpdate.substring(numberInt);
    }
}
