import TestRenderer, { act } from 'react-test-renderer'

import datasetConcepts from '../../DatasetConcepts/__mocks__/datasetConcepts'

import { MIN_WIDTH as PANEL_MIN_WIDTH } from '../../Panel/helpers'

import DatasetLabelsContent from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

jest.mock('next/link', () => 'Link')
jest.mock('../Assets', () => 'DatasetLabelsAssets')

const noop = () => () => {}

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
        .findByProps({ legend: 'Select Scope' })
        .props.onClick({ value: 'TEST' })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/${PROJECT_ID}/datasets/${DATASET_ID}/labels?query=${btoa(
        JSON.stringify({ scope: 'TEST', label: '#All#' }),
      )}`,
    )

    // Select Label
    act(() => {
      component.root
        .findByProps({ label: 'Label:' })
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

  it('should handle filter properly', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[datasetId]',
      query: { projectId: PROJECT_ID, datasetId: DATASET_ID },
    })

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

    // eslint-disable-next-line no-proto
    const spy = jest.spyOn(localStorage.__proto__, 'setItem')

    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'View in Filter Panel' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(spy).toHaveBeenLastCalledWith(
      'rightOpeningPanelSettings',
      JSON.stringify({
        size: PANEL_MIN_WIDTH,
        isOpen: true,
        openPanel: 'filters',
      }),
    )
  })

  it('should handle Add More Labels properly', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[datasetId]',
      query: { projectId: PROJECT_ID, datasetId: DATASET_ID },
    })

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

    // eslint-disable-next-line no-proto
    const spy = jest.spyOn(localStorage.__proto__, 'setItem')

    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'Add More Labels' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(spy).toHaveBeenCalledWith(
      'leftOpeningPanelSettings',
      JSON.stringify({
        size: PANEL_MIN_WIDTH,
        isOpen: true,
        openPanel: 'assetLabeling',
      }),
    )

    expect(spy).toHaveBeenLastCalledWith(
      `AssetLabelingContent.${PROJECT_ID}`,
      `{"datasetId":"${DATASET_ID}","labels":{},"isLoading":false,"errors":{}}`,
    )
  })
})
