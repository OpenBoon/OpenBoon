import TestRenderer from 'react-test-renderer'

import bboxAsset, { boxImagesResponse } from '../../Asset/__mocks__/bboxAsset'

import AnalysisClassification from '../Classification'

const PROJECT_ID = '00000000-0000-0000-0000-000000000000'
const ASSET_ID = bboxAsset.id

describe('<AnalysisClassification />', () => {
  it('should render properly when it is the first item', () => {
    require('next/router').__setUseRouter({
      query: { id: ASSET_ID, projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: boxImagesResponse })

    const component = TestRenderer.create(
      <AnalysisClassification
        moduleName="zvi-object-detection"
        moduleIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when it is not the first item', () => {
    require('next/router').__setUseRouter({
      query: { id: ASSET_ID, projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: boxImagesResponse })

    const component = TestRenderer.create(
      <AnalysisClassification
        moduleName="zvi-object-detection"
        moduleIndex={1}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
