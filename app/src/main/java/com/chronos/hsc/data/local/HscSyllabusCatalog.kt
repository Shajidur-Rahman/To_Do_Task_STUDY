package com.chronos.hsc.data.local

/**
 * Official NCTB HSC Syllabus Chapter Catalog in Bangla.
 * Maps every HscSubject + HscPaper to its exact chapter list in Bangla.
 */
object HscSyllabusCatalog {

    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBanglaDigits(number: Int): String {
        return number.toString().map { ch ->
            if (ch in '0'..'9') banglaDigits[ch - '0'] else ch
        }.joinToString("")
    }

    fun getChaptersFor(subject: HscSubject, paper: HscPaper): List<String> {
        val effectivePaper = if (subject.isSinglePaperOnly) HscPaper.SINGLE else paper
        return when (subject) {
            HscSubject.PHYSICS -> when (effectivePaper) {
                HscPaper.SECOND_PAPER -> physicsSecondPaper
                else -> physicsFirstPaper
            }
            HscSubject.CHEMISTRY -> when (effectivePaper) {
                HscPaper.SECOND_PAPER -> chemistrySecondPaper
                else -> chemistryFirstPaper
            }
            HscSubject.HIGHER_MATH -> when (effectivePaper) {
                HscPaper.SECOND_PAPER -> higherMathSecondPaper
                else -> higherMathFirstPaper
            }
            HscSubject.BIOLOGY -> when (effectivePaper) {
                HscPaper.SECOND_PAPER -> biologySecondPaper
                else -> biologyFirstPaper
            }
            HscSubject.ICT -> ictSinglePaper
            HscSubject.BANGLA -> when (effectivePaper) {
                HscPaper.SECOND_PAPER -> banglaSecondPaper
                else -> banglaFirstPaper
            }
            HscSubject.ENGLISH -> when (effectivePaper) {
                HscPaper.SECOND_PAPER -> englishSecondPaper
                else -> englishFirstPaper
            }
        }
    }

    fun getChapterNameBangla(
        subject: HscSubject,
        paper: HscPaper,
        chapterNumber: Int
    ): String {
        val chapters = getChaptersFor(subject, paper)
        return chapters.getOrNull(chapterNumber - 1)
            ?: "অধ্যায় ${toBanglaDigits(chapterNumber)}"
    }

    fun getDropdownChapterLabel(
        subject: HscSubject,
        paper: HscPaper,
        chapterNumber: Int
    ): String {
        val name = getChapterNameBangla(subject, paper, chapterNumber)
        return "অধ্যায় ${toBanglaDigits(chapterNumber)}: $name"
    }

    // =========================================================================
    // 1. PHYSICS (পদার্থবিজ্ঞান)
    // =========================================================================
    private val physicsFirstPaper = listOf(
        "ভৌত জগৎ ও পরিমাপ",
        "ভেক্টর",
        "গতিবিদ্যা",
        "নিউটনিয়ান বলবিদ্যা",
        "কাজ, শক্তি ও ক্ষমতা",
        "মহাকর্ষ ও অভিকর্ষ",
        "পদার্থের গাঠনিক ধর্ম",
        "পর্যাবৃত্ত গতি",
        "তরঙ্গ",
        "আদর্শ গ্যাস ও গ্যাসের গতিতত্ত্ব"
    )

    private val physicsSecondPaper = listOf(
        "তাপগতিবিদ্যা",
        "স্থির তড়িৎ",
        "চল তড়িৎ",
        "তড়িৎ প্রবাহের চৌম্বক ক্রিয়া ও চুম্বকত্ব",
        "তাড়িতচৌম্বক আবেশ ও পরিবর্তী প্রবাহ",
        "জ্যামিতিক আলোকবিজ্ঞান",
        "ভৌত আলোকবিজ্ঞান",
        "আধুনিক পদার্থবিজ্ঞানের সূচনা",
        "পরমাণুর মডেল এবং নিউক্লিয়ার পদার্থবিজ্ঞান",
        "সেমিকন্ডাক্টর ও ইলেকট্রনিক্স",
        "জ্যোতির্বিজ্ঞান"
    )

    // =========================================================================
    // 2. CHEMISTRY (রসায়ন)
    // =========================================================================
    private val chemistryFirstPaper = listOf(
        "ল্যাবরেটরির নিরাপদ ব্যবহার",
        "গুণগত রসায়ন",
        "মৌলের পর্যায়বৃত্ত ধর্ম ও রাসায়নিক বন্ধন",
        "রাসায়নিক পরিবর্তন",
        "কর্মমুখী রসায়ন"
    )

    private val chemistrySecondPaper = listOf(
        "পরিবেশ রসায়ন",
        "জৈব রসায়ন",
        "পরিমাণগত রসায়ন",
        "তড়িৎ রসায়ন",
        "অর্থনৈতিক রসায়ন"
    )

    // =========================================================================
    // 3. HIGHER MATH (উচ্চতর গণিত)
    // =========================================================================
    private val higherMathFirstPaper = listOf(
        "ম্যাট্রিক্স ও নির্ণায়ক",
        "ভেক্টর",
        "সরলরেখা",
        "বৃত্ত",
        "বিন্যাস ও সমাবেশ",
        "ত্রিকোণমিতিক অনুপাত",
        "সংযুক্ত কোণের ত্রিকোণমিতিক অনুপাত",
        "ফাংশন ও ফাংশনের লেখচিত্র",
        "অন্তরীকরণ",
        "যোগজীকরণ"
    )

    private val higherMathSecondPaper = listOf(
        "বাস্তব সংখ্যা ও অসমতা",
        "যোগাশ্রয়ী প্রোগ্রাম",
        "জটিল সংখ্যা",
        "বহুপদী ও বহুপদী সমীকরণ",
        "দ্বিপদী বিস্তৃতি",
        "কনিক",
        "বিপরীত ত্রিকোণমিতিক ফাংশন ও ত্রিকোণমিতিক সমীকরণ",
        "স্থিতিবিদ্যা",
        "সমতলে বস্তুকণার গতি",
        "বিস্তার পরিমাপ ও সম্ভাবনা"
    )

    // =========================================================================
    // 4. BIOLOGY (জীববিজ্ঞান)
    // =========================================================================
    private val biologyFirstPaper = listOf(
        "কোষ ও এর গঠন",
        "কোষ বিভাজন",
        "কোষ রসায়ন",
        "অণুজীব",
        "শৈবাল ও ছত্রাক",
        "ব্রায়োফাইটা ও টেরিডোফাইটা",
        "নগ্নবীজী ও আবৃতবীজী উদ্ভিদ",
        "টিস্যু ও টিস্যুতন্ত্র",
        "উদ্ভিদ শারীরতত্ত্ব",
        "উদ্ভিদ প্রজনন",
        "জীবপ্রযুক্তি",
        "জীবের পরিবেশ, বিস্তার ও সংরক্ষণ"
    )

    private val biologySecondPaper = listOf(
        "প্রাণীর বিভিন্নতা ও শ্রেণিবিন্যাস",
        "প্রাণীর পরিচিতি (হাইড্রা, ঘাসফড়িং, রুই মাছ)",
        "মানব শারীরতত্ত্ব: পরিপাক ও শোষণ",
        "মানব শারীরতত্ত্ব: রক্ত ও সঞ্চালন",
        "মানব শারীরতত্ত্ব: শ্বসন ও শ্বাসক্রিয়া",
        "মানব শারীরতত্ত্ব: বর্জ্য ও নিষ্কাশন",
        "মানব শারীরতত্ত্ব: চলন ও অঙ্গচালনা",
        "মানব শারীরতত্ত্ব: সমন্বয় ও নিয়ন্ত্রণ",
        "মানব জীবনের ধারাবাহিকতা",
        "মানবদেহের প্রতিরক্ষা",
        "জিনতত্ত্ব ও বিবর্তন",
        "প্রাণীর আচরণ"
    )

    // =========================================================================
    // 5. ICT (তথ্য ও যোগাযোগ প্রযুক্তি)
    // =========================================================================
    private val ictSinglePaper = listOf(
        "তথ্য ও যোগাযোগ প্রযুক্তি: বিশ্ব ও বাংলাদেশ প্রেক্ষিত",
        "কমিউনিকেশন সিস্টেমস ও নেটওয়ার্কিং",
        "সংখ্যা পদ্ধতি ও ডিজিটাল ডিভাইস",
        "ওয়েব ডিজাইন পরিচিতি এবং HTML",
        "প্রোগ্রামিং ভাষা",
        "ডেটাবেজ ম্যানেজমেন্ট সিস্টেম"
    )

    // =========================================================================
    // 6. BANGLA (বাংলা)
    // =========================================================================
    private val banglaFirstPaper = listOf(
        "অপরিচিতা (গদ্য)",
        "বিলাসী (গদ্য)",
        "আমার পথ (গদ্য)",
        "মানব-কল্যাণ (গদ্য)",
        "মাসি-পিসি (গদ্য)",
        "বায়ান্নর দিনগুলো (গদ্য)",
        "রেইনকোট (গদ্য)",
        "সোনার তরী (কবিতা)",
        "বিদ্রোহী (কবিতা)",
        "প্রতিদান (কবিতা)",
        "তাহারেই পড়ে মনে (কবিতা)",
        "আঠারো বছর বয়স (কবিতা)",
        "ফেব্রুয়ারি ১৯৬৯ (কবিতা)",
        "লালসালু (উপন্যাস)",
        "সিরাজউদ্দৌলা (নাটক)"
    )

    private val banglaSecondPaper = listOf(
        "বাংলা উচ্চারণের নিয়ম",
        "বাংলা বানানের নিয়ম",
        "বাংলা ভাষার ব্যাকরণিক শব্দশ্রেণি",
        "বাংলা শব্দ গঠন (উপসর্গ ও প্রত্যয়)",
        "সমাস",
        "বাক্যতত্ত্ব",
        "বাংলা ভাষার অপপ্রয়োগ ও শুদ্ধ প্রয়োগ",
        "পারিভাষিক শব্দ ও অনুবাদ",
        "দিনলিপি, অভিজ্ঞতা, ভাষণ ও প্রতিবেদন",
        "বৈদ্যুতিন চিঠি ও আবেদনপত্র",
        "সারাংশ, সারমর্ম ও ভাবসম্প্রসারণ",
        "সংলাপ, খুদে গল্প ও প্রবন্ধ রচনা"
    )

    // =========================================================================
    // 7. ENGLISH (ইংরেজি)
    // =========================================================================
    private val englishFirstPaper = listOf(
        "ইতিহাস গড়ার মানুষ ও প্রতিষ্ঠান (People Making History)",
        "শিক্ষা ও জীবন (Education & Life)",
        "স্বপ্ন ও তারুণ্য (Dreams & Youth)",
        "মানবিক সম্পর্ক ও বয়ঃসন্ধিকাল (Adolescence)",
        "মানবাধিকার ও শান্তি (Human Rights & Peace)",
        "পরিবেশ ও প্রকৃতি (Environment & Nature)",
        "শিল্পকলা, সংগীত ও লোকশিল্প (Art & Craft)",
        "পুরাণ ও সাহিত্য (Myths & Literature)",
        "প্যাসেজ, ফ্লো-চার্ট ও সামারি (Comprehension & Summary)",
        "শূন্যস্থান পূরণ ও রি-অ্যারেঞ্জ (Cloze Test & Rearranging)",
        "প্যারাগ্রাফ, স্টোরি ও চিঠি (Paragraph, Story & Letter)",
        "গ্রাফ-চার্ট ও থিম রাইটিং (Graph/Chart & Theme)"
    )

    private val englishSecondPaper = listOf(
        "আর্টিকেল ও প্রিপোজিশন (Articles & Prepositions)",
        "বিশেষ শব্দ ও ফ্রেজের ব্যবহার (Words & Phrases)",
        "অসম্পূর্ণ বাক্য সম্পূর্ণকরণ (Completing Sentences)",
        "ভার্বের সঠিক রূপ (Right Form of Verbs)",
        "ন্যারেশন / উক্তি পরিবর্তন (Narrative Style)",
        "মডিফায়ার (Modifiers)",
        "সেনটেন্স কানেক্টর (Sentence Connectors)",
        "সমার্থক ও বিপরীতার্থক শব্দ (Synonym & Antonym)",
        "বিরামচিহ্ন (Punctuation)",
        "আবেদনপত্র ও ইমেইল (Formal Letter & Email)",
        "প্রতিবেদন ও অনুচ্ছেদ রচনা (Report & Paragraph)",
        "রচনা লিখন (Composition / Essay)"
    )
}
