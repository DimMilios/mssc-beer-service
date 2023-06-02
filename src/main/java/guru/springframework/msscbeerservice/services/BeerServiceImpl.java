package guru.springframework.msscbeerservice.services;

import guru.springframework.msscbeerservice.domain.Beer;
import guru.springframework.msscbeerservice.repositories.BeerRepository;
import guru.springframework.msscbeerservice.web.controller.NotFoundException;
import guru.springframework.msscbeerservice.web.mappers.BeerMapper;
import guru.springframework.msscbeerservice.web.model.BeerDto;
import guru.springframework.msscbeerservice.web.model.BeerPagedList;
import guru.springframework.msscbeerservice.web.model.BeerStyleEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by jt on 2019-06-06.
 */
@RequiredArgsConstructor
@Service
public class BeerServiceImpl implements BeerService {
    private final BeerRepository beerRepository;
    private final BeerMapper beerMapper;

    @Cacheable(cacheNames = "beerCache", key = "#beerId", condition = "#showInventoryOnHand == false ")
    @Override
    public BeerDto getById(UUID beerId, Boolean showInventoryOnHand) {
        Beer beer = beerRepository.findById(beerId).orElseThrow(NotFoundException::new);
        return showInventoryOnHand ? beerMapper.beerToBeerDtoWithInventory(beer) : beerMapper.beerToBeerDto(beer);
    }

    @Override
    public BeerDto saveNewBeer(BeerDto beerDto) {
        return beerMapper.beerToBeerDtoWithInventory(beerRepository.save(beerMapper.beerDtoToBeer(beerDto)));
    }

    @Override
    public BeerDto updateBeer(UUID beerId, BeerDto beerDto) {
        Beer beer = beerRepository.findById(beerId).orElseThrow(NotFoundException::new);

        beer.setBeerName(beerDto.getBeerName());
        beer.setBeerStyle(beerDto.getBeerStyle().name());
        beer.setPrice(beerDto.getPrice());
        beer.setUpc(beerDto.getUpc());

        return beerMapper.beerToBeerDtoWithInventory(beerRepository.save(beer));
    }

    @Cacheable(cacheNames = "beerListCache", condition = "#showInventoryOnHand == false ")
    @Override
    public BeerPagedList listBeers(String beerName, BeerStyleEnum beerStyle, Boolean showInventoryOnHand, PageRequest pageRequest) {
        Page<Beer> beerPage;
        if (!StringUtils.isEmpty(beerName) && !StringUtils.isEmpty(beerStyle)) {
            beerPage = beerRepository.findAllByBeerNameAndBeerStyle(beerName, beerStyle, pageRequest);
        } else if (!StringUtils.isEmpty(beerName) && StringUtils.isEmpty(beerStyle)) {
            beerPage = beerRepository.findAllByBeerName(beerName, pageRequest);
        } else if (StringUtils.isEmpty(beerName) && !StringUtils.isEmpty(beerStyle)) {
            beerPage = beerRepository.findAllByBeerStyle(beerStyle, pageRequest);
        } else {
            beerPage = beerRepository.findAll(pageRequest);
        }

        Function<Beer, BeerDto> dtoMapFunction = showInventoryOnHand
                ? beerMapper::beerToBeerDtoWithInventory
                : beerMapper::beerToBeerDto;

        return new BeerPagedList(beerPage
                .getContent()
                .stream()
                .map(dtoMapFunction)
                .collect(Collectors.toList()),
                PageRequest.of(beerPage.getPageable().getPageNumber(), beerPage.getPageable().getPageSize()),
                beerPage.getTotalElements());
    }

    @Cacheable(cacheNames = "beerUpcCache")
    @Override
    public BeerDto getByUpc(String upc) {
        Beer beer = beerRepository.findByUpc(upc).orElseThrow(NotFoundException::new);
        return beerMapper.beerToBeerDto(beer);
    }
}
