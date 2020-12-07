const imageAsset = {
  id: 'AjXYVpaVeLsOgpenKKSW8oDB5YuOTWDs',
  metadata: {
    system: {
      jobId: 'c01b9495-def1-133b-9f27-aa2fbe1ff2ea',
      dataSourceId: 'e7a2bba3-5b29-14a0-a447-2a2d393b335b',
      timeCreated: '2020-05-28T17:40:25.959286Z',
      state: 'Analyzed',
      projectId: 'a0952c03-cc04-461c-a367-9ffae8c4199a',
      timeModified: '2020-05-28T17:42:02.078469Z',
      taskId: 'c01b9496-def1-133b-9f27-aa2fbe1ff2ea',
    },
    files: [
      {
        size: 226582,
        name: 'image_1024x687.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/AjXYVpaVeLsOgpenKKSW8oDB5YuOTWDs/proxy/image_1024x687.jpg',
        category: 'proxy',
        attrs: { width: 1024, height: 687 },
      },
      {
        size: 64837,
        name: 'image_512x343.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/AjXYVpaVeLsOgpenKKSW8oDB5YuOTWDs/proxy/image_512x343.jpg',
        category: 'proxy',
        attrs: { width: 512, height: 343 },
      },
      {
        size: 69600,
        name: 'web-proxy.jpg',
        mimetype: 'image/jpeg',
        id: 'assets/AjXYVpaVeLsOgpenKKSW8oDB5YuOTWDs/web-proxy/web-proxy.jpg',
        category: 'web-proxy',
        attrs: { width: 1024, height: 687 },
      },
    ],
    source: {
      path: 'gs://zorroa-dev-data/image/pluto_2.1.jpg',
      extension: 'jpg',
      filename: 'pluto_2.1.jpg',
      checksum: 1241849316,
      mimetype: 'image/jpeg',
      filesize: 65649,
    },
    metrics: {
      pipeline: [
        {
          executionTime: 0.1,
          module: 'standard',
          checksum: 2178814325,
          executionDate: '2020-05-28T17:40:28.912023',
          processor: 'zmlp_core.core.PreCacheSourceFileProcessor',
        },
        {
          executionTime: 0.19,
          module: 'standard',
          checksum: 117837444,
          executionDate: '2020-05-28T17:40:29.568083',
          processor: 'zmlp_core.core.FileImportProcessor',
        },
        {
          executionTime: 4.0,
          module: 'standard',
          checksum: 457707303,
          executionDate: '2020-05-28T17:40:33.825286',
          processor: 'zmlp_core.proxy.ImageProxyProcessor',
        },
        {
          executionTime: 0,
          module: 'standard',
          checksum: 482873147,
          processor: 'zmlp_core.proxy.VideoProxyProcessor',
        },
        {
          executionTime: 0.54,
          module: 'standard',
          checksum: 1879445844,
          executionDate: '2020-05-28T17:41:59.185759',
          processor: 'zmlp_analysis.zvi.ZviSimilarityProcessor',
        },
      ],
    },
    media: {
      orientation: 'landscape',
      aspect: 1.49,
      width: 1072,
      length: 1,
      type: 'image',
      height: 720,
    },
    analysis: {
      'zvi-image-similarity': {
        type: 'similarity',
        simhash:
          'ICPCCPDOPPGPPHBCJEPLPAIPLPPNJPPPFBEPNGPAFPHMPNDPKMEDBGPCPOJMBCCGDPPPPPHDPBHAAPPAAAFPEMPPAAAAGFPPGFPPPDPADKFADPGHFFGPEBAAOPPGIEDPPHPIHAPPHAPPPDNIOOPOMDDPIDPAGHPAPPNPIIBPEAEDPJABGOANPPGPPJPPDADBAJAPBDPJABBNPPIPFABPPLJJBPAAOHHPNJPBFAPFPCAPPPNMGCOAAPBMPPPALPEJBAPABOPPABLJPEDAHAPPCHHPPIMBPPHDPPLALPPFPPCOGCCHAAFGPGAPPPAOLPPAPAPDEAIPPNJFHGBPJDAPBPHIIAPPPHGEPCPPAAHPEPFHDAECPPKJAHAPPLAPBAAPIPNCKPAGJKPPPLBHPANAAPLPKBIGAFPBHEPMPHGEAPFAJDAPMPLPPPKCBPOALPAEPPGNIPPPPBPAEKPMBHHBPAPPPPGBPEKPPPHJIPPHPPDPCBDLPPKPKBCFPAJMPDBPHBAPPPJAPPBBEPFMAPAPPEDPOJAEBGGPPAHPCPMADIPGAPAHCPGKCBPPAMDPPAAAPECPFPPHMJOAPDAPAMFPLPALHAPHAKPLDCKPPDADAPGLJPPFBOPGBPHFDAHPOHPPPPFAAMEPAAPDOADAONFPPPPDPCPJPDAPAAAFGJPMMKPEAIGAIKPPEPADAAAPBAAKADHPDHIBAPNFEFAJCCGAIPPPPPAEPAIALHEEDGFPAPAPDIPPGABBAPEPFIBEACPIEAEMPCLBPDAPPAPKPFJLBIHPIPBOALPAAGIPBCECMPCPHKPKADPPPPCPPPDHAIAPBPPNCPCEJDEBAPAPEPPABHAPHPGPIDPPPAPNBGEAACDAPPFBPPAPAPGPAIPBPKPJPDAJDBCAPPBHBJPIPMBPPALOGFKBBLPPAPFPPOBFPPAPPAMPIIPPGPPLBPKAJPCPDJAAFGPHAPPAAPAPDEPABPPDCMGPBMPPPPPPPLLPKBPFPPFEAIHPJPANJPFNPKAMHIJPPPPOPEPJHPCAPACPMAPCBCPMPFAEJOBPBFAFCBPPDEPPFPBPDJGPOPPAKAPAAAJCJOGPPNHCCEPEBPPGKPOMCGPAPPAAPBPBFGKJMNPLMPPAPHPAHAPHAPJKPGJPGBPPPBJGPPALFMEGNPJPAAPBCGPPJDPHAAKJPFABMDAPHPHABDADNABPPEFCCLEPPAHMCPLBEFAIIPDADFOAFPPADEPEAPDLKCPPLKPGDPBPDGIPACFPAHAPBHPACINBBPPPGKALCPJAFPBPIEBIAPELPAPPFBOFCPJJPNPIAAPHIPJJABCPPAPAMFICNKPPLPEGJOKIINAMBPCNGAPKJKPDPPHAPDNPPNFGJBPLEPONCADEPDBPPPPPPKPOALOPHBECDNDPMEGBEDKKPPPPBAEPPLDPPPPAFCPPGBPKAFPODBPPPJPPPAAPNPEDPPBPNEALPEABEIAPPAPPMLAPDDJPAPHPLPBFHALPPAPFPEPPOGPPPGCPBPPPAADAPONJBPPDPKHPPPBFCKKPPPPPPAPIJPDPOPPPPHOAPPOPPAPGDPDPGLPEAHGEOLFBAGAMPNLIPPJBJGBPNPFEAPPCPDPPBLPPBIPAAIDPPJFKPAEPKNAAEEIAMPGNPBMLEMBAPIHGFPPPGEAKBIHKACPPPMPPPAPPMPBBJPBPPPIPAEEFJDLBPPPPGLPFFPAHBAAHBAHPAHKDPAABPAPPPPAPGLJBPPAEPEGKKOAPPAAMACPCMDPAEAEJAPPJEPPPHAPPPGIGPPCOPHGPPOPMBNPFFBEPGPAAFPOAIPLBIAJMPAEEPPPPPAEHABPOEBPCMFIPPAGHLKBHAPPEPIPPBLAPLAGPDPPPEPPEHPDAAAEBABPEAPAAAEAPPGDPLMIEAMFPEAAHKFGKAKDPPMDAPPPKLADPPPCJPPPPPOPPBJPPPPPFCIABPFPAGPBDPJDPKPPNABBBAPPPPDPPDPMJGCDCIPAEPADEGBPPAJNPBCPBPPBFPOCPBFACLNPOBJBAJMIH',
      },
    },
    clip: {
      sourceAssetId: 'AjXYVpaVeLsOgpenKKSW8oDB5YuOTWDs',
      stop: 1.0,
      pile: 'zeRwdqpC6Pqyp6dlq4VoI5PJPzs',
      start: 1.0,
      length: 1.0,
      type: 'page',
    },
  },
}

export default imageAsset
