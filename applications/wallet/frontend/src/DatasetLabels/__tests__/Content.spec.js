import TestRenderer, { act } from 'react-test-renderer'

import datasetConcepts from '../../DatasetConcepts/__mocks__/datasetConcepts'

import DatasetLabelsContent from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

jest.mock('../Assets', () => 'DatasetLabelsAssets')

describe('<DatasetLabelsContent />', () => {
  it('should render properly', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({ data: datasetConcepts })

    const component = TestRenderer.create(
      <DatasetLabelsContent
        projectId={PROJECT_ID}
        datasetId={DATASET_ID}
        query=""
        page={1}
        datasetName="cats"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Select Scope
    act(() => {
      component.root
        .findByProps({ label: 'Scope' })
        .props.onChange({ value: 'TEST' })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/${PROJECT_ID}/datasets/${DATASET_ID}/labels?query=${btoa(
        JSON.stringify({ scope: 'TEST', label: 'tabby' }),
      )}`,
    )

    // Select Label
    act(() => {
      component.root
        .findByProps({ label: 'Label' })
        .props.onChange({ value: 'calico' })
    })

    expect(mockRouterPush).toHaveBeenLastCalledWith(
      `/${PROJECT_ID}/datasets/${DATASET_ID}/labels?query=${btoa(
        JSON.stringify({ scope: 'TRAIN', label: 'calico' }),
      )}`,
    )
  })

  it('should render properly without labels', () => {
    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <DatasetLabelsContent
        projectId={PROJECT_ID}
        datasetId={DATASET_ID}
        query=""
        page={1}
        datasetName="cats"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
