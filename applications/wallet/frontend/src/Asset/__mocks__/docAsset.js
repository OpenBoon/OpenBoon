const docAsset = {
  id: 'pNwnXjVntgbDQgPZhkXqVT-2URMqvJNL',
  metadata: {
    system: {
      jobId: '90ca7838-8599-11ea-b5f5-e27f287c1339',
      dataSourceId: '9023ff07-8599-11ea-b5f5-e27f287c1339',
      timeCreated: '2020-04-23T19:36:19.026812Z',
      state: 'Analyzed',
      projectId: '03aa4218-1243-4a62-8113-42064dcf8f0e',
      timeModified: '2020-04-23T19:39:17.115693Z',
      taskId: 'ef99192f-1530-1429-a313-f6aecad56ff6',
    },
    files: [
      {
        size: 203847,
        name: 'image_791x1024.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/pNwnXjVntgbDQgPZhkXqVT-2URMqvJNL/proxy/image_791x1024.jpg',
        category: 'proxy',
        attrs: { width: 791, height: 1024 },
      },
      {
        size: 63009,
        name: 'image_395x512.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/pNwnXjVntgbDQgPZhkXqVT-2URMqvJNL/proxy/image_395x512.jpg',
        category: 'proxy',
        attrs: { width: 395, height: 512 },
      },
      {
        size: 30075,
        name: 'image_247x320.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/pNwnXjVntgbDQgPZhkXqVT-2URMqvJNL/proxy/image_247x320.jpg',
        category: 'proxy',
        attrs: { width: 247, height: 320 },
      },
      {
        size: 62162,
        name: 'web-proxy.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/pNwnXjVntgbDQgPZhkXqVT-2URMqvJNL/web-proxy/web-proxy.jpg',
        category: 'web-proxy',
        attrs: { width: 791, height: 1024 },
      },
    ],
    source: {
      path:
        'gs://zmlp-private-test-data/zorroa-deploy-testdata/zorroa-dev-testdata/zorroa-test-data/office/test_document.docx',
      extension: 'docx',
      filename: 'test_document.docx',
      mimetype:
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    },
    metrics: {
      pipeline: [
        {
          executionTime: 0.4,
          module: 'standard',
          checksum: 2178814325,
          executionDate: '2020-04-23T19:37:03.208034',
          processor: 'boonai_core.core.PreCacheSourceFileProcessor',
        },
        {
          executionTime: 1.23,
          module: 'standard',
          checksum: 3558348737,
          executionDate: '2020-04-23T19:37:06.407931',
          processor: 'boonai_core.core.FileImportProcessor',
        },
        {
          executionTime: 2.93,
          module: 'standard',
          checksum: 457707303,
          executionDate: '2020-04-23T19:37:09.686094',
          processor: 'boonai_core.proxy.ImageProxyProcessor',
        },
        {
          module: 'standard',
          checksum: 482873147,
          processor: 'boonai_core.proxy.VideoProxyProcessor',
        },
        {
          executionTime: 0.6,
          module: 'standard',
          checksum: 1879445844,
          executionDate: '2020-04-23T19:37:44.015925',
          processor: 'boonai_analysis.zvi.ZviSimilarityProcessor',
        },
        {
          executionTime: 0.83,
          module: 'boonai-object-detection',
          checksum: 3329037091,
          executionDate: '2020-04-23T19:38:16.097040',
          processor: 'boonai_analysis.zvi.ZviObjectDetectionProcessor',
        },
        {
          executionTime: 3.57,
          module: 'boonai-label-detection',
          checksum: 2989691564,
          executionDate: '2020-04-23T19:39:08.218578',
          processor: 'boonai_analysis.zvi.ZviLabelDetectionProcessor',
        },
        {
          module: 'boonai-text-detection',
          checksum: 4290842193,
          processor: 'boonai_analysis.zvi.ZviOcrProcessor',
        },
      ],
    },
    media: {
      orientation: 'portrait',
      length: 3,
      width: 612.0,
      timeCreated: '0000-12-30T00:00:00.234Z',
      type: 'document',
      title: 'Three Page Test Document',
      content:
        'Lorem ipsum dolor sit amet by Your Name on September 04 Nam liber tempor cum soluta nobis eleifend option congue nihil imperdiet doming id quod mazim placerat facer possim assum. Duis autem vel eum iriure dolor by Your Name on September 04 Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan. READ MORE ON OUR WEBSITE HeartWorks Inc. 115, SomeCity, PA, 55344',
      height: 792.0,
    },
    analysis: {
      'boonai-label-detection': {
        count: 1,
        type: 'labels',
        predictions: [{ score: 1.0, label: 'web_site' }],
      },
      'boonai-image-similarity': {
        type: 'similarity',
        simhash:
          'POCBPPPAJPFJPAPHJJILIMNPAPGPJPPPDFPJPDPPOFOPGBKDPPCPHDIPPPIPHDKBPAMPPLPOHKPAFPPPHGBKBPPPPHABPHPPBMPPPPPIPNGHJCEOPPPBPPEEPPIPINPFPPLGLCCJBIMPPBHKBCPBCOLCKPPCPPHKIGBJEPFINHNINPBLHEBPGDPHPBHLPJPPPPPIMPPPJPHPEPOKPJELPJPJPFPEDMJJDHJCPHPPCPPDHFCGOMPPHHBOPPJDPFNDANPBPPPIPOLKPMKPPBMGPJBJPBBPLPIPGPLIIMFEPPPLPPADGPPPPGJGIPDLGPPBJCPPPKAKPODGFPDPNPBMPPAPFFPCHFLHPEHBEPPANPPBPBNKPKPIFKPCIPPPEPBPPPLAKDJOCPECOBPJPFJGPFPCPKPKHPPPBPHJGPNPPMOPPDKKLHPGBPKJAPIPKPPCPMCLEPDPPBPPPPODGCAJABHPMPPPJPHFBPPDLPIPJGPLABPGOPJPMKDIPPPOLLPABCAMMPPPEPLOOPNCPPPPPPPPPBKNPPEPBHOPAGIBHPIPNENGKHPJFIGPIOBBPPGDPPJBFPPPPPPPKHGCHDPPPPPGBDDPCFCKDPPHPDPMEJGHHPEPJPPPMPPPEPPKPCOPFPOEMKPLBHPDCADPKNPBAPPBDFJLPFBAPLPJPIMEPHBPPJBGEAPPPBOPCPAANFHPMBFAPIJBPBPFFMPIAPEPPGPNPPEPAHKJFEKHFCDPOLAPKPELPPDPIPEKKPPPLPPCPBIICOKHPPAPPPANPCDDPPPNGECPFIPDAOBPPPPLDAPPHPGPJJEBEOICPPPPOPHDLPGPOBPPPKHPPACPAPPPHDPPBDPDPPPPINPPPJAKBDPCAPIKPFBFKIAAPPIAPPFPLHKILPEOBGDOCCIJEPHPEPJDPKJJIPKJPCDAGGEPPPPGPHPPBOPMFPPAFOABPBJPLPPPPIBPAIEPPEPANPHIEDKKNCDCPPPPNGPIAMPPAKPBFPPPPPJMPPPOACKBAODPJCFEPHIEJDPDNPPBPOPPPJPDPPPPPPEKHPPNPLIFPPPGINEPPKCPICPPNMPEPJPPDDACMPPPCCLPOCPBBPGKPILPPBIPPPCPBEPPPDPPOAABGCEEIKBGBHPPPPPPJIJKGPDGFAFCLGPPNCABPKDPKPCPFNPAPKPPBCEBIPEPPPPPBPGPIPGPPPKPCDHPPGPAKOIGJPPBPPPPPLBMPPPJPECPAKCAEPPPPAPDHBPCKPPPPIMCNAAGDFPCPCCDOBPMHPIBPPPAMPPAADOEPFGPKDPMCIPPMPMPPPDPLHPPIEAFPPPPLPGBOAHHNJFACFDPBAEKBAJPPJAHJPPNPNNIPNIPPCFKAJFPEBPCIDMPPEPBPPOPBIPBCOJCPPKPDPPECPCNDCLCAPEAPNBLIPGPBPPPPFIPPPPPHPPCPPPAGFPLJAPBPBIPPNGNEPPKPLEPIDPMPLHPPPBOCAPEPANBPFHCBEPPPFGPLMPPAPPPDMPMEPPDJGPCAPMPCPANPFAEPCALPNFIPPPPPMPPBLEPJIPGEPAFGCPPOAPPFPPPNAGPPPPAGPPMOKEAJOPPPKPEIBPPPPPJEELBAOCPPBBFCPBDPBPOBFGJKPPEJFAMGDEEOPPPCPPDPKPPPIPPBFKPPCJBOOPNEFPPKAOGPPPDPPEKKIAPPPEPFCPPPIGIKABGPPPHPIPPCPPAPPPCEPIMPOPPDIDDPBPDKIPNHHNPPPFKEPPPEODJCDPCPGCGPFAPCFCJKPPPMPPOKKKEPGPPHHPPPHCNPEDEJPPODPCCJNJDCLIKPPBPGPIPDPPMPGMJPPPOAFAPGDPPPLHBIGPGKHPPPPOPPICMNCLHFPODPCJJDDAHDPPPCPPBPPBNODDPPGOPHPFPPBCOPPCPCPPPMCPAALPPLJFCGPPPPPBPAOPPPLFEPPPKPPHPCCJAJPCAPAPCDFPBKLPPPPPCPBPPPAOMPOPMPKPOGEPJDCPCPAAPPCFPPDCBPPPPLOGHPJEPAEAPAIPAFGPCKPMGPHFE',
      },
    },
    clip: {
      sourceAssetId: 'nn1lq_FCOCT_EY5CnLg1KGgEQPojUk7e',
      stop: 2.0,
      pile: 'al_1yhcFfNJOAt7aoqYZ1aRyvJ4',
      start: 2.0,
      length: 1.0,
      type: 'page',
    },
  },
}

export default docAsset
