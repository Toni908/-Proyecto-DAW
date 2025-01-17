package cat.iesmanacor.backend_private.controller;

import cat.iesmanacor.backend_private.componentes.ComparaDia;
import cat.iesmanacor.backend_private.entities.Horario;
import cat.iesmanacor.backend_private.entities.Periodo;
import cat.iesmanacor.backend_private.entities.Restaurant;
import cat.iesmanacor.backend_private.entities.Useracount;
import cat.iesmanacor.backend_private.services.HorarioService;
import cat.iesmanacor.backend_private.services.PeriodoService;
import cat.iesmanacor.backend_private.services.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;


import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static cat.iesmanacor.backend_private.componentes.User.getUser;

@Controller
@RequestMapping("/restaurant/admin")
public class HorarioController {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private PeriodoService periodoService;

    @Autowired
    private HorarioService horarioService;

    @GetMapping("/horario/{id}")
    public String getPeriodo(@PathVariable(value = "id") BigInteger id, Model model, HttpServletRequest request){
        Optional<Restaurant> restaurant = restaurantService.findRestaurantById(id);
        String name = "Periodos de " + restaurant.get().getNombre();
        restaurant.get().setNombre(name);
        model.addAttribute("restaurant", restaurant.get());

        Useracount user = getUser(request);

        if(user == null || !restaurant.get().getUseracount().equals(user)){
            return "redirect:/error/401";
        }

        return "periodos";
    }

    @GetMapping("/horario/delete/{id}")
    public String deletePeriodo(@PathVariable(value = "id") Long id, Model model, HttpServletRequest request){
        Optional<Periodo> periodo = periodoService.findById(id);

        Useracount user = getUser(request);

        if(user == null || !periodo.get().getRestaurant().getUseracount().equals(user)){
            return "redirect:/error/401";
        }

        periodoService.deleteById(id);

        return "nada";
    }

    @GetMapping("/horario/edit/periodo/{id}")
    public String editPeriodo(@PathVariable(value = "id") Long id, Model model, HttpServletRequest request){
        Optional<Periodo> periodo = periodoService.findById(id);
        model.addAttribute("periodo", periodo.get());

        Useracount user = getUser(request);

        if(user == null || !periodo.get().getRestaurant().getUseracount().equals(user)){
            return "redirect:/error/401";
        }

        Date dateStart = periodo.get().getFecha_inicio();
        Date dateEnd = periodo.get().getFecha_fin();

        String dateStartS = dateStart.toString();
        String dateEndS = dateEnd.toString();

        String[] dateStarts = dateStartS.split("-");
        String[] dateEnds = dateEndS.split("-");

        String start = dateStarts[1] + "/" + dateStarts[2] + "/" + dateStarts[0];
        String dateValue = start + " - " + dateEnds[1] + "/" + dateEnds[2] + "/" + dateEnds[0];

        model.addAttribute("dateValue", dateValue);
        model.addAttribute("start", start);

        return "periodo_modify";
    }

    @GetMapping("/horario/create/periodo/{id}")
    public String createPeriodo(@PathVariable(value = "id") BigInteger id, Model model, HttpServletRequest request){
        Periodo periodo = new Periodo();
        Optional<Restaurant> restaurant = restaurantService.findRestaurantById(id);
        periodo.setRestaurant(restaurant.get());
        model.addAttribute("periodo", periodo);
        model.addAttribute("restaurant", periodo.getRestaurant());

        Useracount user = getUser(request);

        if(user == null || !restaurant.get().getUseracount().equals(user)){
            return "redirect:/error/401";
        }

        return "periodo_modify";
    }

    boolean isWithinRange(Date testDate, Date startDate, Date endDate) {
        return !(testDate.before(startDate) || testDate.after(endDate));
    }

    @PostMapping("/horario/periodo/save/{id}")
    public String savePeriodo(@PathVariable(value = "id") BigInteger id, @RequestParam("daterange") String dateRange, @Valid Periodo periodo, BindingResult result, Model model, HttpServletRequest request){
        Optional<Restaurant> restaurant = restaurantService.findRestaurantById(id);
        periodo.setRestaurant(restaurant.get());
        List<Periodo> periodos = restaurant.get().getPeriodos();
        String url = "redirect:/restaurant/admin/horario/"+ id;

        Useracount user = getUser(request);

        if(user == null || !restaurant.get().getUseracount().equals(user)){
            return "redirect:/error/401";
        }

        if(periodo.getId_periodo() != null){
            Optional<Periodo> p = periodoService.findById(periodo.getId_periodo());
            periodo.setHorarios(p.get().getHorarios());
        }

        String[] splited = dateRange.split("\\s+");

        String dateStarts = splited[0];
        String dateEnds = splited[2];

        boolean used = false;

        try {
            java.util.Date dateStartd = new SimpleDateFormat("yyyy-MM-dd").parse(dateStarts);
            Date dateStart = new java.sql.Date(dateStartd.getTime());
            java.util.Date dateEndd = new SimpleDateFormat("yyyy-MM-dd").parse(dateEnds);
            Date dateEnd = new java.sql.Date(dateEndd.getTime());

            for(Periodo per : periodos){
                if (per.getId_periodo() != periodo.getId_periodo()) {
                    String s = dateStarts;
                    String e = dateEnds;

                    LocalDate start = LocalDate.parse(s);
                    LocalDate end = LocalDate.parse(e);

                    List<LocalDate> totalDates = new ArrayList<>();

                    Date ps = per.getFecha_inicio();
                    Date pe = per.getFecha_fin();

                    while (!start.isAfter(end)) {
                        totalDates.add(start);
                        java.util.Date datest = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
                        Date dateStartss = new java.sql.Date(datest.getTime());
                        if (isWithinRange(dateStartss, ps, pe)) {
                            used = true;
                            break;
                        }
                        start = start.plusDays(1);
                    }
                }
            }

            periodo.setFecha_inicio(dateStart);
            periodo.setFecha_fin(dateEnd);

            if(used) {
                model.addAttribute("dateValue", dateRange);
                model.addAttribute("error", "El periodo no puede coincidir con otros periodos de tu restaurante");
                model.addAttribute("periodo", periodo);
                model.addAttribute("restaurant", periodo.getRestaurant());
                if(periodo.getId_periodo() != null){
                    return "horarios";
                }
                return "periodo_modify";
            }

        }catch(ParseException e){
            System.out.println("Espero que nada vaya aqui nunca");
        }

        if(periodo.getId_periodo() != null) {
            url = "redirect:/restaurant/admin/periodo/horario/"+ periodo.getId_periodo();
        }

        periodoService.save(periodo);

        return url;

    }

    // horarios

    @GetMapping("/periodo/horario/{id}")
    public String getHorario(@PathVariable(value = "id") Long id, Model model, HttpServletRequest request){
        Optional<Periodo> periodo = periodoService.findById(id);
        List<String> list = new ArrayList<>();

        Useracount user = getUser(request);

        if(user == null || !periodo.get().getRestaurant().getUseracount().equals(user)){
            return "redirect:/error/401";
        }

        Collections.sort(periodo.get().getHorarios(), new ComparaDia());

        Date dateStart = periodo.get().getFecha_inicio();
        Date dateEnd = periodo.get().getFecha_fin();

        String dateStartS = dateStart.toString();
        String dateEndS = dateEnd.toString();

        String dateValue = dateStartS + " - " + dateEndS;

        model.addAttribute("dateValue", dateValue);
        model.addAttribute("start", dateStartS);
        model.addAttribute("restaurant", periodo.get().getRestaurant());
        model.addAttribute("periodo", periodo.get());
        model.addAttribute("horarios", list);

        return "horarios";
    }

    @GetMapping("/periodo/horario/delete/{id}")
    public String deleteHorario(@PathVariable(value = "id") Long id, HttpServletRequest request){
        Optional<Horario> horario = horarioService.findById(id);

        Useracount user = getUser(request);

        if(user == null || !horario.get().getPeriodo().getRestaurant().getUseracount().equals(user)){
            return "redirect:/error/401";
        }

        horarioService.deleteById(id);

        return "nada";
    }

    @GetMapping("/periodo/horario/create/{id}")
    public String createHorario(@PathVariable(value = "id") Long id, Model model, HttpServletRequest request){
        Horario horario = new Horario();
        horario.setPeriodo(periodoService.findById(id).get());

        Useracount user = getUser(request);

        if(user == null || !periodoService.findById(id).get().getRestaurant().getUseracount().equals(user)){
            return "redirect:/error/401";
        }


        model.addAttribute("horario", horario);

        return "horario_modify";
    }

    @GetMapping("/periodo/horario/edit/{id}")
    public String editHorario(@PathVariable(value = "id") Long id, Model model, HttpServletRequest request){
        Optional<Horario> horario = horarioService.findById(id);
        model.addAttribute("horario", horario.get());

        Useracount user = getUser(request);

        if(user == null || !horario.get().getPeriodo().getRestaurant().getUseracount().equals(user)){
            return "redirect:/error/401";
        }


        return "horario_modify";
    }

    @PostMapping("/periodo/horario/save/{id}")
    public String saveHorario(@PathVariable(value = "id") Long id, WebRequest request, @RequestParam("checkbox") List<String> listaDias, HttpServletRequest requesthttp, Model model){
        Horario horario = new Horario();

        Optional<Periodo> periodop = periodoService.findById(id);

        Useracount user = getUser(requesthttp);

        if(user == null || !periodop.get().getRestaurant().getUseracount().equals(user)){
            return "redirect:/error/401";
        }

        String id_horario = request.getParameter("id_horario");

        Time start = java.sql.Time.valueOf(request.getParameter("dateStart"));
        horario.setHora_inicio(start);
        Time end = java.sql.Time.valueOf(request.getParameter("dateEnd"));
        horario.setHora_fin(end);
        Optional<Periodo> periodo = periodoService.findById(id);
        horario.setPeriodo(periodo.get());

        List<String> days = new ArrayList<>();

        if(id_horario != null){
            horario.setId_horario(Long.parseLong(id_horario));
            horario.setDay(request.getParameter("dateChange"));
            if(verificarHorario(horario, periodop.get())){
                horarioService.save(horario);
            }else{
                model.addAttribute("horario", horario);
                model.addAttribute("error","Error, el horario que estas intentando introducir coincide con otro del " + horario.getDay());
                return "horario_modify";
            }

        }else{
            List<Horario> listhora = new ArrayList<>();
            for(int x = 1 ; x < listaDias.size() ; x++){
                Horario h = new Horario();
                h.setHora_inicio(start);
                h.setHora_fin(end);
                h.setPeriodo(periodo.get());
                h.setDay(listaDias.get(x));
                if(verificarHorario(horario, periodop.get())){
                    listhora.add(h);
                }else{
                    model.addAttribute("horario", h);
                    model.addAttribute("errorc","Error, el horario que estas intentando introducir coincide con otro del " + h.getDay());
                    return "horario_modify";
                }
            }
            for( Horario hs : listhora){
                horarioService.save(hs);
            }
            /*if(days.size() == 1){
                Horario k = new Horario();
                k.setPeriodo(periodop.get());
                model.addAttribute("horario", k);
                model.addAttribute("errorc","Error, uno de los horarios que estas intentando introducir coincide con uno ya existente. En concreto el dia " +
                        days.get(0) + ". Los demas dias han sido añadidos correctamente");
                return "horario_modify";
            }else if(days.size() > 1){
                String dais = days.get(0);
                for(int x = 1 ; x < days.size() ; x++){
                    dais = dais + ", " + days.get(x);
                }
                Horario k = new Horario();
                k.setPeriodo(periodop.get());
                model.addAttribute("horario", k);
                model.addAttribute("errorc","Multiples dias coinciden con otros, especificamente los siguientes: " + dais + ". Los demas han sido añadidos correctamente");
                return "horario_modify";
            }
             */
        }

        return "redirect:/restaurant/admin/periodo/horario/" + id;
    }

    public boolean verificarHorario(Horario horario, Periodo p){
        String[] Starth = horario.getHora_inicio().toString().split(":");
        String[] Endh = horario.getHora_fin().toString().split(":");
        int Sh = Integer.parseInt(Starth[0]);
        int Eh = Integer.parseInt(Endh[0]);
        if(Eh == 0){
            Eh = 24;
        }
        String dayh = horario.getDay();
        for(Horario h : p.getHorarios()){
            String day = h.getDay();
            if(day.equals(dayh)){
                String[] Start = h.getHora_inicio().toString().split(":");
                String[] End = h.getHora_fin().toString().split(":");
                int S = Integer.parseInt(Start[0]);
                int E = Integer.parseInt(End[0]);
                if(E == 0){
                    E = 24;
                }
                for(int x = Sh ; x <= Eh ; x++){
                    if(S <= x && E >= x){
                        return false;
                    }
                }
            }
        }

        return true;
    }

}

