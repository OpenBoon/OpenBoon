const bboxAsset = {
  id: 'HfQ4xpvClTgRHFidgyXRcK01e6u239Lv',
  metadata: {
    system: {
      jobId: 'f37942bd-1a85-13ea-8acb-d205ab8f4b5d',
      dataSourceId: 'f37942ba-1a85-13ea-8acb-d205ab8f4b5d',
      timeCreated: '2020-04-23T17:43:23.940464Z',
      state: 'Analyzed',
      projectId: '00000000-0000-0000-0000-000000000000',
      timeModified: '2020-04-23T17:45:25.375590Z',
      taskId: 'f37942be-1a85-13ea-8acb-d205ab8f4b5d',
    },
    files: [
      {
        size: 28648,
        name: 'image_283x178.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/HfQ4xpvClTgRHFidgyXRcK01e6u239Lv/proxy/image_283x178.jpg',
        category: 'proxy',
        attrs: {
          width: 283,
          height: 178,
        },
      },
      {
        size: 7719,
        name: 'web-proxy.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/HfQ4xpvClTgRHFidgyXRcK01e6u239Lv/web-proxy/web-proxy.jpg',
        category: 'web-proxy',
        attrs: {
          width: 283,
          height: 178,
        },
      },
    ],
    source: {
      path: 'gs://zorroa-dev-data/image/frog.jpeg',
      extension: 'jpeg',
      filename: 'frog.jpeg',
      checksum: 810206371,
      mimetype: 'image/jpeg',
      filesize: 7555,
    },
    metrics: {
      pipeline: [
        {
          executionTime: 0.09,
          module: 'standard',
          checksum: 2178814325,
          executionDate: '2020-04-23T17:43:28.248368',
          processor: 'boonai_core.core.PreCacheSourceFileProcessor',
        },
        {
          executionTime: 0.16,
          module: 'standard',
          checksum: 117837444,
          executionDate: '2020-04-23T17:43:30.819801',
          processor: 'boonai_core.core.FileImportProcessor',
        },
        {
          executionTime: 0.66,
          module: 'standard',
          checksum: 457707303,
          executionDate: '2020-04-23T17:43:41.308661',
          processor: 'boonai_core.proxy.ImageProxyProcessor',
        },
        {
          module: 'standard',
          checksum: 482873147,
          processor: 'boonai_core.proxy.VideoProxyProcessor',
        },
        {
          executionTime: 0.56,
          module: 'standard',
          checksum: 1879445844,
          executionDate: '2020-04-23T17:44:52.332863',
          processor: 'boonai_analysis.zvi.ZviSimilarityProcessor',
        },
        {
          executionTime: 0.22,
          module: 'boonai-text-detection',
          checksum: 4290842193,
          executionDate: '2020-04-23T17:44:54.720051',
          processor: 'boonai_analysis.zvi.ZviOcrProcessor',
        },
        {
          executionTime: 0.86,
          module: 'boonai-object-detection',
          checksum: 3329037091,
          executionDate: '2020-04-23T17:45:07.981961',
          processor: 'boonai_analysis.zvi.ZviObjectDetectionProcessor',
        },
        {
          executionTime: 0.67,
          module: 'boonai-label-detection',
          checksum: 2989691564,
          executionDate: '2020-04-23T17:45:23.523104',
          processor: 'boonai_analysis.zvi.ZviLabelDetectionProcessor',
        },
      ],
    },
    media: {
      orientation: 'landscape',
      aspect: 1.59,
      width: 283,
      length: 1,
      type: 'image',
      height: 178,
    },
    analysis: {
      'boonai-object-detection': {
        count: 1,
        type: 'labels',
        predictions: [
          {
            score: 0.824,
            bbox: [0.223, 0.253, 0.901, 0.921],
            label: 'banana',
          },
        ],
      },
      'boonai-label-detection': {
        count: 1,
        type: 'labels',
        predictions: [
          {
            score: 0.96,
            label: 'tailed_frog',
            tags: ['frog', 'banana'],
          },
          {
            score: 0.86,
            label: 'cherry',
          },
        ],
      },
      'boonai-text-detection': {
        count: 0,
        type: 'content',
        content: '',
      },
      'boonai-image-similarity': {
        type: 'similarity',
        simhash:
          'FBIJPPAIPBPPAPPPBDPLPICAHBDIDPPAAHPPFPBHIPBBLKBOHJJEPAPPBCABCDAPCPBHCCCCHIKEDPEFNPPPPGOPAHKBIAKPFEPFPGGJPPPCPPFPPPLFLBCAKOPNFPBFGPPFCMIBPJJMCGPDKPKDMPPOPODJDGMBDPPAFBJIPBPBCGEFHMBBBPCPIAEKMGPPPPPEBPIGEDAOHDPCPCPAMPALPJIDPGPGDPPHODHPPPPJAGPAPPPNKKPDPPDPPPJPPLPLPPFGBFPPPPMJPPGBHEPPAKPPPPPBLMMBMDPHJPDPPBCALMHOHPPPEPIIKIPMKPDJGEPHPPDPPIDBBCPPCPPIDJLPCPPDFFANJPPDCPFPMAPPPPPAPMKPPCFPABPBGBHPCFPPPAJPDBDNPPFMHMFMBLAPFHFOLJPJAPJKMDNHBPABEGIJPCPFEPJPIPPLPAPGILPEPPHFAGPPPMDFCAPPAPKNDKPPLGJEDNDHGLCPDIPJBPAKAFOPCKIIPEPFIFAPJPCLFJOPLAPPPHPIPCNCHHIPAPDNPALEPGDEGCFCEPLHPJJPENEPFDPGMAKABLOIPGMPCPJPABPPGPFDKJHAFGPPPDEAODPPPPPPJPBPBALIOCMPCLPPJFOPMCDGBCDPHFGDKPJPOAPPCPPCAMPJKOHEPGHCPOGOBHPADPKPEDPAPPAGNPNPKPAKGPCPDPPIKGPPHBKGKGOPICACPPFPPAADPEJIKBIEPPGPPJIGPHMLOGKAPPPLALJHNNPPGAPPAAOPPEBPKBAGBPEPCPEHPPPIPFPPEGJBKDPHPPIPMPBDAEBPJIGCPOOPKOLPFHDHOCAMLCNAPDNGMEHDPPPPAPPEPCCPIGBPPPAPAIPPPONPPPPPPAONICPOPMEBJPMPBEPCNAIECAEBPPPHHINPEABPPPEOEIAAEPPLPNPAPFIPBPPBGHPPCPJAPAPGAHPPLODAPPPBPCEAEPBDDGKGPDFLDPPCPHPPNDPEABPPPDEKAPPPPPPICADCPFFPPBEPDEPPNHMPOPMEGCNFBEFAIFPCFHPEFPPPNJBAEABKPCPJCPDFIPCPGCENMKJECELHPAPPJCNPAEHHBFAEPPAACIPBAPFBDNPAEEIPPAEMMBBPKBENKPFLPDBPPELJPBAICPPBEPNFDGPHDPJJPAHLDNBAPPPJPMLKBPNBPCKKJFLOMHIOJNPPPPPBBBBMPAAOOBPEOKGPCKKPAPFPDPPLPCICODPKBDGMPPPFBPKEBGIPDBACBPEPJPEDJPPEPPPDBPPIIOPFPCFKLDCPOOLLPCHPGLPHFPPKDPNPPPAJJPPPJDIPPFPBPHPKBEBIDDEFOPPPOHMFPBBPPPKPACPAOPEPPFJPNFBHPGDGADPEHBPKPHLEOADBJPPIIPPGBCABAPPGPIPDPNBPAGLKPFCEHIAIPPPPFJKBPCNIGGBPPPBBNPKDMIPLPHPIPLGCPFBPLMNLPIIPAIPELPGPHPPIPPJEPPAAMPMKPGPCBPAPPMPABAHBPFAODNOEKCOGPPGKDHBCPCJEPBIALBDJPPAEPAPGPCPPLADHBGPPLNGHAGPAEPPEHPOPDGPKAMGPPDGIBPFJBDPIBPJPPPCPDPKIFDHCDPGFPMGHPCBPIJMPFDCPLJBEPPPPDKLAJMPBAPIBPBGMICGCDGBNPOAAEJMKPPHPKPPAHMMPKKLPEHPPNKPEPDLPHBPCCPPPHPNPPJBDBCPFCPOCPPEPHNEPNEPKPAAPPPAFFPPAFBEPBHMDCDIPPPFPGEAFBPGGIKBLPFCPGHOJJPEOAMBACCCGAEEPPPPDPPBPPOICPLEEAPABPCPPKPPBPPENDPCHABIDMPPPPPPPIOOCFPPPBHCKEJNGEPEPPGODFPPDKFFNPBEPFIPPCEDOPBPDEOKDOLDDPHCAICAPLCPHPDPPIDHPPLPLJAPECPHCIPAPPHPPPBPCPDPCJIAHMPDFOJAPHPPOGABBPCNPKNPEFHCPPIPFPMPCAPPFALPBPHADPEPAKPECDCPOGPPEPGPOPKMPHPPG',
      },
    },
    clip: {
      sourceAssetId: 'HfQ4xpvClTgRHFidgyXRcK01e6u239Lv',
      stop: 1,
      pile: 'AjIFnRgUrzm25de5iHcUKOvzlVE',
      start: 1,
      length: 1,
      type: 'page',
    },
  },
}

export const boxImagesResponse = {
  'boonai-object-detection': {
    count: 2,
    type: 'labels',
    predictions: [
      {
        score: 0.824,
        bbox: [0.223, 0.253, 0.901, 0.921],
        label: 'banana',
        b64Image:
          'data:image/png;base64, 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',
      },
      {
        score: 0.924,
        bbox: [0.223, 0.253, 0.901, 0.921],
        label: 'frog',
        b64Image:
          'data:image/png;base64, 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',
      },
    ],
  },
}

export default bboxAsset
