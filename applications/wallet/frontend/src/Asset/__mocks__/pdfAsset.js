const pdfAsset = {
  id: 'BLNIfseda4sj4peU0JJUOR4eViw5yvXp',
  metadata: {
    system: {
      jobId: '90ca7838-8599-11ea-b5f5-e27f287c1339',
      dataSourceId: '9023ff07-8599-11ea-b5f5-e27f287c1339',
      timeCreated: '2020-04-23T19:36:20.373558Z',
      state: 'Analyzed',
      projectId: '03aa4218-1243-4a62-8113-42064dcf8f0e',
      timeModified: '2020-04-23T19:42:57.969278Z',
      taskId: 'ef99192f-1530-1429-a313-f6aecad56ff6',
    },
    files: [
      {
        size: 156426,
        name: 'image_723x1024.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/BLNIfseda4sj4peU0JJUOR4eViw5yvXp/proxy/image_723x1024.jpg',
        category: 'proxy',
        attrs: {
          width: 723,
          height: 1024,
        },
        url:
          'https://wallet.zmlp.zorroa.com/api/v1/projects/03aa4218-1243-4a62-8113-42064dcf8f0e/assets/BLNIfseda4sj4peU0JJUOR4eViw5yvXp/files/category/proxy/name/image_723x1024.jpg',
      },
      {
        size: 46244,
        name: 'image_361x512.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/BLNIfseda4sj4peU0JJUOR4eViw5yvXp/proxy/image_361x512.jpg',
        category: 'proxy',
        attrs: {
          width: 361,
          height: 512,
        },
        url:
          'https://wallet.zmlp.zorroa.com/api/v1/projects/03aa4218-1243-4a62-8113-42064dcf8f0e/assets/BLNIfseda4sj4peU0JJUOR4eViw5yvXp/files/category/proxy/name/image_361x512.jpg',
      },
      {
        size: 20232,
        name: 'image_226x320.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/BLNIfseda4sj4peU0JJUOR4eViw5yvXp/proxy/image_226x320.jpg',
        category: 'proxy',
        attrs: {
          width: 226,
          height: 320,
        },
        url:
          'https://wallet.zmlp.zorroa.com/api/v1/projects/03aa4218-1243-4a62-8113-42064dcf8f0e/assets/BLNIfseda4sj4peU0JJUOR4eViw5yvXp/files/category/proxy/name/image_226x320.jpg',
      },
      {
        size: 56535,
        name: 'web-proxy.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/BLNIfseda4sj4peU0JJUOR4eViw5yvXp/web-proxy/web-proxy.jpg',
        category: 'web-proxy',
        attrs: {
          width: 723,
          height: 1024,
        },
        url:
          'https://wallet.zmlp.zorroa.com/api/v1/projects/03aa4218-1243-4a62-8113-42064dcf8f0e/assets/BLNIfseda4sj4peU0JJUOR4eViw5yvXp/files/category/web-proxy/name/web-proxy.jpg',
      },
    ],
    source: {
      path:
        'gs://zmlp-private-test-data/zorroa-deploy-testdata/zorroa-dev-testdata/zorroa-test-data/office_import/The_Little_Prince.pdf',
      extension: 'pdf',
      filename: 'The_Little_Prince.pdf',
      mimetype: 'application/pdf',
      url:
        'https://wallet.zmlp.zorroa.com/api/v1/projects/03aa4218-1243-4a62-8113-42064dcf8f0e/assets/BLNIfseda4sj4peU0JJUOR4eViw5yvXp/files/source/The_Little_Prince.pdf',
    },
    metrics: {
      pipeline: [
        {
          executionTime: 0.41,
          module: 'standard',
          checksum: 2178814325,
          executionDate: '2020-04-23T19:41:33.397702',
          processor: 'zmlp_core.core.PreCacheSourceFileProcessor',
        },
        {
          executionTime: 0.03,
          module: 'standard',
          checksum: 3558348737,
          executionDate: '2020-04-23T19:41:35.437001',
          processor: 'zmlp_core.core.FileImportProcessor',
        },
        {
          executionTime: 2.52,
          module: 'standard',
          checksum: 457707303,
          executionDate: '2020-04-23T19:41:38.150475',
          processor: 'zmlp_core.proxy.ImageProxyProcessor',
        },
        {
          module: 'standard',
          checksum: 482873147,
          processor: 'zmlp_core.proxy.VideoProxyProcessor',
        },
        {
          executionTime: 0.63,
          module: 'standard',
          checksum: 1879445844,
          executionDate: '2020-04-23T19:41:59.403514',
          processor: 'zmlp_analysis.zvi.ZviSimilarityProcessor',
        },
        {
          executionTime: 0.91,
          module: 'zvi-object-detection',
          checksum: 3329037091,
          executionDate: '2020-04-23T19:42:18.509436',
          processor: 'zmlp_analysis.zvi.ZviObjectDetectionProcessor',
        },
        {
          executionTime: 3.6,
          module: 'zvi-label-detection',
          checksum: 2989691564,
          executionDate: '2020-04-23T19:42:52.894550',
          processor: 'zmlp_analysis.zvi.ZviLabelDetectionProcessor',
        },
        {
          module: 'zvi-text-detection',
          checksum: 4290842193,
          processor: 'zmlp_analysis.zvi.ZviOcrProcessor',
        },
      ],
    },
    media: {
      orientation: 'portrait',
      author: 'Bomoo.com',
      length: 89,
      width: 595,
      timeCreated: '2003-07-31T20:34:34Z',
      type: 'document',
      title: 'The Little Prince',
      content:
        ' Chapter 23 the little prince encounters a merchant "Good morning," said the little prince. "Good morning," said the merchant. This was a merchant who sold pills that had been invented to quench thirst. You need only swallow one pill a week, and you would feel no need of anything to drink. "Why are you selling those?" asked the little prince. "Because they save a tremendous amount of time," said the merchant. "Computations have been made by experts. With these pills, you save fifty-three minutes in every week." "And what do I do with those fifty-three minutes?" "Anything you like..." "As for me," said the little prince to himself, "if I had fifty-three minutes to spend as I liked, I should walk at my leisure toward a spring of fresh water." 68 ',
      height: 842,
    },
    analysis: {
      'zvi-label-detection': {
        count: 2,
        type: 'labels',
        predictions: [
          {
            score: 0.565,
            label: 'web_site',
          },
          {
            score: 0.431,
            label: 'menu',
          },
        ],
      },
      'zvi-image-similarity': {
        type: 'similarity',
        simhash:
          'APOKLPPEEPHLKDPPMHBKFBOPABPPNPOPCMPPPDPPCDCJEAHBPPEPEFJPPPEPBIABLEBPONIPAPPJPPPPGBAPNLDPPLKCPPPNDFHKKPNHPFHPPPFGIPPAPPDCHPPPPGPOPPCCPANMPHKPHAPPCGEAAPILPPPEPHCGDADFPDAPPKPMBHBMPBIPCFPIFEPDPFPGPPLFEAPPPPMPCPOJPPFPPAPPODLBAPGPJAPIPLPJCPKBJOPFPOPPPEFPPPLBPKGFGPPJPHPKPPGBPOIPPDBMPJDGPEIPFPABKPPPPDNCPKPDOPCMKPPPPPFBFLDHJPPOHMPPIPCCPMCACPGPDPAGMPAPLAPDEMHGPFBBADABIDIPGEGBPLPPEAIPPPPBKPAMPLMAPABHBJFDBFPMPKJCPLKLAPGAPPPPIPBAGFIKCJOLPAPFOOHDHPFFBDIPAPPAPPHKAPAJPPPMPPHBOCDPGCPPFPJPECDKAPDKBKKPPEPMCEPPLPCPPPEMPPPPPAPJGAACKPPPEPEPPPCAFEMPPIPPPDEPPGPPAILPGBCBPCICMKBNCPOOHFGFGPIPPPAPFPGBAPBLMIPBMCGELMPPPPLIBIDPBCDCBPIBPHNKHKJEOMGPGPDEPPNLIKPDIKPPBHGCPPLKBIPDFAFPHNPBAPHCBMPHNAMDPFPOPBAPLDAPPMDGLEPPHBEPOPDEDEDOLAGHPAHLAFENJDPOHFCPPHPJAFFAAPIECPCKCFGGBBBEKLAHPPFIIPNPPHPPGPPBPDCDBOPGAPBPHPBPPFIJPJPHDPEPBAPCBPHPKPPKBAMPMPGAPGLBBHPEHPPMPDAFILMPKPPPIJAPPDDIEHPPPMPEBPICGPHPPPPNPCAPCBGDHPAPPCPADDACFOFBNHCPAEEIPPPMBDBGAABBCHCPPHECPNEICBKIAADBPBAPPMHKPPPPAAPFKEACGPAOPPGKMPPPPCPBABCPHBPBFPGELABJEDFBPPAPFJMJAPPLALNMCJPPGLCOPPPKDACCCLMPFBPBPLCFPLPLBPJBIPPOPFPBPIKNPPBDCKPCPPALPKPFICCPPOGMLCGBFKPNPGEPLHEGPIPPFJIKPCPBAPMBPHPPOAAJPPPCEDPPMAPAPAEGJAFAEPAEEEPPMPPPPPNAFMNAKBCFHPPPNABNNBDPAPDPHHECPPPPFONBDKLPPGPPDKOJFIFPDPOMHAAPPJPFJNNEJPPEPHEFNEBPGOPPPNEPFCDFDGPPNBEPHAPAPJPKPDBFFBAABPPDPBELPANCKGICNPFEHPPNAACBPCMPPDPPLBPPPPPHIPPOAEPPIDALPOPIPOIBCCEHBEFCHHEPBBJFFFPPPJCLFPIKOFPHJPIFPCHGAHPPDPLIDDPPPPPAPPPPCPPBDMAMPCPPBCMAKPHOCKFGAPHFDKCOPBCPCPPLPPFPPHPPIPOAIPPDDOPIDAJEPCAPPHLMKOIPPDDKBDPAPPBLPPELCBPAPAPADBCANPGJNGHFMDODHPPPFLKPCPPKHBHCAKHMMPCPPCDGFMJMPPICPIPOPPPNAAAKLEJDFPAAHAJPPGAPBOPIPEEPPAPNJIPKPADPHPOOHJPJPCJNKPPDCAPCFPPPPJBPFEAAPAPBEBAHBPPPPCAMPIPBBFBPELPCGDPPPBGPAFKPLGBFNHGPADPPBABDHPPKPMAFFBBPPPJPPDPDMBGEPHHKKJPPKEGJPPOAJGMCGGFIPOPPHGDAPEJLLDKCIPHMPPCLKPPKBODPLPBAJMIPPOBEILPHPPPGPHPLINGJKPNJPFLPPLBPPKDPCNOJKPAEPMOOPGHJCPIPCGCPCPHHPNOMPPNBAAAPPBPPPPDAFEPBPDPGPPIMFHCKCFLPEIPCIABMEDBJPKHPBDPGOPGJMAPPPPNIPKIPPDIPOLJPJFPPFBPJGGPCBPGGFPJEPJCPBPFPCPBOPFKCPPPPFKOBPPDBPBPACPPJBHPPOMMBPBPPPAHPKLPPPPPOKGPBBAPBACHMPABPPKIAPFPPPHBFPPNPIABPACPPAEMPEPPNPJPB',
      },
    },
    clip: {
      sourceAssetId: 'bDZHK1X50dDgJ3QfRkLFVyPLnvbXSNOP',
      stop: 72,
      pile: 'm0-4BOCEEGfchcxHtYrBD99DrfY',
      start: 72,
      length: 1,
      type: 'page',
    },
  },
  url:
    'https://wallet.zmlp.zorroa.com/api/v1/projects/03aa4218-1243-4a62-8113-42064dcf8f0e/assets/BLNIfseda4sj4peU0JJUOR4eViw5yvXp/',
}

export default pdfAsset
