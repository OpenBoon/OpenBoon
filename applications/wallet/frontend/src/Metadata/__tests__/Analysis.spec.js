import TestRenderer from 'react-test-renderer'

import bboxAsset from '../../Asset/__mocks__/bboxAsset'

import MetadataAnalysis from '../Analysis'

jest.mock('../Analysis/Classification', () => 'MetadataAnalysisClassification')

const PROJECT_ID = '00000000-0000-0000-0000-000000000000'
const ASSET_ID = bboxAsset.id

describe('<MetadataAnalysis />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      query: { id: ASSET_ID, projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: bboxAsset })

    const component = TestRenderer.create(<MetadataAnalysis />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
