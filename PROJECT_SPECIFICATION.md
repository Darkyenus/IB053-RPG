# Browser / Chat game

Jednoduchá hra ve stylu sfgame, hratelná jak z prohlížeče,
tak z mobilních chat aplikací (Telegram, možná FB Messenger).

## Koncept
- **Oblasti**:
	- Svět se skládá z oblastí
	- Příklady oblastí: "Town of Tortuga", "Dark Forest", "Deep Cavern", ...
	- Mají jméno, popis, seznam monster která se zde nacházejí a odkaz na hřbitov, na kterém se hráč ocitne když zemře
	- Také mají seznam oblastí, kam se dá jít dál a popis této cesty (*"Cestovat zkrz bránu na louku za městem"*)
	- Každá oblast má svůj popisek pro hráče: "Tortuga: na to jak je město malé, je překvapivě rušné.
    		Přístav místní krčmy neustále zásobuje raubíři, kořalkou a touhou po zisku"
    - V každé obytné oblasti (města, úkryty, apod.) je pro hráče které se v oblasti nacházejí chat,
    		tj. můžou si spolu povídat (později se může přidat obchod mezi hráči, nebo společná dobrodružství)
    - Občas se může v oblasti stát nějaká událost (možná, ve městě se na dočansou dobu otevře
    		stánek se vzácným zbožím, atd.)
- **Aktivity**:
	- Každý hráč se vždy věnuje nějaké aktivitě
	- Aktivita určuje prezentaci a **akce** které se zde dají dělat (nakupovat u vendora, otevřít truhlu, vyhledat boj, ...)
	- Aktivita určuje, co se stane při vstupu, odchodu a sertvávání v akci
		- Při vstupu/odchodu do/z nebezpečné zóny je šance na napadení monstrem
		- Při pobytu ve městě se postava pomalu léčí
		- atd.
	- K aktivitám patří: boj, nakupování, procházení inventáře a výběr toho, co si hráč oblékne,
    		vylepšování schopností postavy (lvl-up), **Pobyt v oblasti je také aktivita, a to výchozí**
    - Může trvat jakkoli dlouho **(na vstup od hráče se může čekat dlouho, protože je zamýšleno aby se hra hrála
        i po malých částech kdy má zrovna hráč čas, proto by bylo dobře, kdyby z obrazovky bylo vždy jasné co se
        s hráčem zrovna děje, i když naposledy hrál před týdnem a už si nepamatuje kam se dostal)**
- **Akce**
	- Je vždy v rámci aktivity, která ji nabízí
	- Jednorázová změna, která se může projevit změnou lokace, aktivity, předmětů, ...
	- Například akce aktivity *boj*: Bojuj, uteč, vypij lektvar
- **Událost**
	- Projevuje se v UI jako položka v seznamu událostí, který je nezávislý na aktivitě či dalších faktorech
	- Informuje hráče o tom co se s ním nebo ve světě stalo
	- Hráč proto vždy může vidět alespoň nějaké množství posledních událostí
- **Hráč**:
	- Má schopnosti a předměty (inventář a obléknuté), někde se nachází, vykonává nějakou aktivitu, která mu umožňuje vykonat akci
	- Inventář je neomezený, ale příliš mnoho předmětů v inventáři zpomaluje
		(-agility stat, pokud předmětů nad nějakou hranici)
	- Pokud zemře, musí buď počkat věčnost (několik minut?), nebo ztratit zkušenosti (jen pokud je má)
- **Nepřítel**:
	- Monstrum, zvíře, bandita, ..., má také staty, ale místo předmětů (může mít teoreticky nějaké oblečené pokud se jedná o humanoida),
		má loot-table, tj. po vítězství se vygeneruje loot
	- K boji dochází jen v akci Boj
- **Staty**:
	- Číselné hodnoty, které určují schopnosti postavy, předmětu, nepřítele, atd.. Vyšší je vždy lepší
	- **Síla (Strength)**
		- Primární stat pro sílu útoku
		- Zvyšuje (logaritmicky nebo podobně) sílu útoku zbraně
	- **Obratnost (Dexterity)**
		- Poměr obratnost útočníka a obránce určuje pravděpodobnost zásahu
	- **Hbitost (Agility)**
		- Určuje jak často má postava příležitost útočit (používat schopnosti?)
		- Body agility se přičítají do initiative counteru
		- Ten kdo má větší iniciativu je na tahu a jeho tahem se příčte agility do iniciativy protivníka
		- Kolik se přičte do iniciativy je i malou mírou ovlivněno štěstím
	- **Štěstí (Luck)**
		- Skrytý stat, hráč ho nemůže vidět v UI
		- Zvyšuje možnost náhle znatelně lepšího výsledku - lepší loot, šance na kritický zásah, atd.
		- Nabývá hodnot od 0 (incl) do 100 (incl)
		- "Moje luck 100 + protivníkův luck 0 => 60% šance na šťastnou událost"
		- "Moje luck 100 + protivníkův luck 100 => 20% šance na šťastnou událost"
    - **Odolnost (Stamina)**
        - Určuje počet životů (pro hráče přímo závislé na levelu, pro zjednodušení)
	- Základní hodnota je určená úrovní postavy, respektive tím jak si rozdělí své
		body schopností získané při zvýšení úrovně (řekl bych, že 10 pointů na level, přemýšlím kolik budou dostávat nepřátelé, aby jejich level vypovídal o tom jak sjou silní ale aby jsi zase neměl extra velký problém s tí zabít jednoho)
	- Dále se může jejich hodnota dočasně měnit pomocí předmětů s pasivními bonusy, pomocí dočasných
		stavových efektů ("Napil jsi se kořalky, 30 tahů budš mít +5 síly ale -5 obratnosti")

## Implementace

- Rozdělená na jádro a frontendy
- **Jádro**
	- Stará se o logiku hry, komunikuje s frontendy ve kterých je hráč zrovna aktivní
	- Obsahuje a načítá databázi oblastí, předmětů, nepřátel, atd., s vazbami do kódu
    - Udržuje stav světa, provádí simulace, stará se o persistenci, autoritativní
    - Obsahuje event loop, který zpracovává herní logiku a komunikuje s frontendy
        - Je na něj možné poslat událost co se má stát v budoucnu
- **Frontend**
	- Stará se o komunikaci mezi jádrem a hráči, prezentace herního stavu a předávání vstupu od hráčů zpět jádru
	- 1 Frontend pro web, 1 pro Telegram, atd.
	- Dostávají přes callbacky informace pro hráče
	- K jádru může být připojeno i více frontendů najednou