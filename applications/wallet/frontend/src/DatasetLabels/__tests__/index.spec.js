import TestRenderer from 'react-test-renderer'

import dataset from '../../Dataset/__mocks__/dataset'

import DatasetLabels from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

jest.mock('../Content', () => 'DatasetLabelsContent')

describe('<DatasetLabels />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]/labels',
      query: {
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: dataset })

    const component = TestRenderer.create(<DatasetLabels />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
