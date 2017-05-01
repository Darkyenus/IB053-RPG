# Browser / Chat game

Jednoduchá hra ve stylu sfgame, hratelná jak z prohlížeče,
tak z mobilních chat aplikací (Telegram, možná FB Messenger).

## Koncept
- Svět je rozdělen do oblastí, ve kterých se může hráč nacházet.
	- Příklad oblastí: "Town of Tortuga", "Dark Forest", "Deep Cavern", ...
	- Oblasti mají:
		- Akce které se zde dají dělat (nakupovat u vendora, otevřít truhlu, vyhledat boj, ...)
		- Akce které se samy spustí při vstupu/odchodu(/možná pobytu) v oblasti
			- Při vstupu/odchodu do/z nebezpečné zóny je šance na napadení monstrem
			- Při pobytu ve městě se postava pomalu léčí
		- Každá oblast má svůj popisek pro hráče: "Tortuga: na to jak je město malé, je překvapivě rušné.
			Přístav místní krčmy neustále zásobuje raubíři, kořalkou a touhou po zisku"
		- V každé obytné oblasti (města, úkryty, apod.) je pro hráče které se v oblasti nacházejí chat,
			tj. můžou si spolu povídat (později se může přidat obchod mezi hráči, nebo společná dobrodružství)
		- Seznam oblastí, kam se dá jít dál
		- Občas se může v oblasti stát nějaká událost (možná, ve městě se na dočansou dobu otevře
			stánek se vzácným zbožím, atd.)
	- Akce:
		- Kromě úvodního popisku/událostí Oblastí (a nabídky akcí), se o kompletní interakci s uživatelem stará
		 Akce. Prezentuje možnosti akce, pokud nějaké jsou, popisuje uživateli situaci, atd.
		- K akcím patří: boj, nakupování, (možná chat? bylo by asi lepší kdyby chat byl nad akcemi,
			podle možností cílových platforem), procházení inventáře a výběr toho, co si hráč oblékne,
			vylepšování schopností postavy (lvlup), **Pobyt v oblasti (pseudoakce, defaultní)**
		- Akce samotná určuje kdy skončí, či co se s hráčem stane
		- Může trvat jakkoli dlouho **(na vstup od hráče se může čekat dlouho, protože je zamýšleno aby se hra hrála
			i po malých částech kdy má zrovna hráč čas, proto by bylo dobře, kdyby z obrazovky bylo vždy jasné co se
			s hráčem zrovna děje, i když naposledy hrál před týdnem a už si nepamatuje kam se dostal)**
        (Počet akcí typu cestování/boj/průzkum/léčení bych omezil něčím jako jsou tahy - systém že se ti třeba každých 30 minut vygeneruje tah a můžeš si uchovat třeba 100 tahů)
	- Hráč:
		- Má schopnosti a předměty (inventář a obléknuté), někde se nachází a prožívá nějakou akci
		- Inventář je neomezený, ale příliš mnoho předmětů v inventáři zpomaluje
			(-agility stat, pokud předmětů nad nějakou hranici)
		- Pokud zemře, ztrácí *něco* (zkušenosti? předměty? (dobré by bylo kdyby šly předměty získat zpět,
		ztráta zkušeností je lepší), možná nějaký čas ("Musíš počkat X minut dokud se nezotavíš ze setkání se smrtí"))
        (určitě jsem pro ztrátu zkušeností.)
	- Monstrum:
		- Nepřítel, má také staty, ale místo předmětů (může mít teoreticky nějaké oblečené pokud se jedná o humanoida),
			má loot-table, tj. po vítězství se vygeneruje loot
            (bude se muset upravovat pro level hráče + luck, asi bych byl pro standartní příponový + item level systém generování, s tím, že druh nepřítele ovlivnovnuje spíš typy věcí co z něj padají, v případě rare nepřátel asi nějaký bonus k item levelu)
		- K boji dochází jen v akci Boj
	- Staty:
		- Číselné hodnoty, které určují schopnosti postavy. Vyšší je vždy lepší
		- **Síla (Strength)**: Primární stat pro útok, zvyšuje poškození po zásahu (+1)
		- **Obratnost (Dexterity)**: Poměr obratnost útočníka a obránce určuje pravděpodobnost zásahu
        (hodil by se vzorec kdy je vždycky určitá šance (byť malá), že zasáhne + potom diminishing return toho, že budeš stackovat obratnost)
		- **Hbitost (Agility)**: Určuje jak často má postava příležitost útočit (používat schopnosti?)
        (takže z toho budou něco jako action pointy per turn, right? )
		- **Štěstí (Luck)**: Zvyšuje možnost náhle znatelně lepšího výsledku - lepší loot, šance na kritický 
            (do toho se započítává i štěstí protivníka), šance na výprodej v obchodě...
            (to vliv tohohle už je teoreticky započítán tím, že má šanci ti taky dát kritický zásah v závistosti na jeho luck + mitigation už je. + asi taky diminishing returns na crit)
        - **Odolnost (Stamina)** Určuje počet životů (možná přímo závislé na levelu, pro zjednodušení)
		- Základní hodnota je určená úrovní postavy, respektive tím jak si rozdělí své
			body schopností získané při zvýšení úrovně (řekl bych, že 10 pointů na level, přemýšlím kolik budou dostávat nepřátelé, aby jejich level vypovídal o tom jak sjou silní ale aby jsi zase neměl extra velký problém s tí zabít jednoho)
		- Dále se může jejich hodnota dočasně měnit pomocí předmětů s pasivními bonusy, pomocí dočasných
			stavových efektů ("Napil jsi se kořalky, 30 tahů budš mít +5 síly ale -5 obratnosti")

## Implementace

- Rozdělené do jádra a frontendů
	- Jádro se stará o logiku hry, komunikuje s frontendem do kterého je hráč zrovna přihlášen (max 1)
	- Více frontendů bude pravděpodobně řešeno přes jeden "proxy" frontend, do kterého se zapojí všechny ostatní
	- 1 Frontend pro web, 1 pro Telegram, atd.
	- Frontend se stará o prezentaci herního stavu a dalších možností
	- Jádro s frontendy komunikuje přes zprávy, do jádra jdou zprávy o akcích, do frontendu jdou zprávy o vizuálních změnách
- Zpráva
	- Nese info mezi core a frontend
	- Typy:
		- Uživatelská akce: od klienta k jádru, o tom že uživatel zvolil prezentovanou akci
		- Zobrazení: od jádra ke klientovi, o tom že se změnilo něco z vizuální reprezentace
			- Výběru akcí - změnilo se, co může uživatel udělat
			- Flavor text - změnilo se, co obecně uživatel vidí, pravděpodobně formátované
			- Událostní - stala se událost, která se nedá dlouhodobě sledovat, ale stojí za povšimnutí:
				krok boje, chat, událost uvnitř oblasti, může vést ke skutečné push notifikaci
			- *Bylo by možné zkombinovat flavor a události pro lepší vzhled? (hyperlinky) Investigate!*
- Jádro
	- Obsahuje databázi oblastí, předmětů, akcí, s vazbami do kódu
		(modulární by bylo dobré pro rychlý vývoj dalších předmětů, oblastí, akcí...)
	- Udržuje stav světa, provádí simulace, silně autoritativní