import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'
import datasets from '../__mocks__/datasets'

import User from '../../User'

import Datasets from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = datasets.results[0].id

jest.mock('next/link', () => 'Link')

describe('<Datasets />', () => {
  it('should let the user start labeling', async () => {
    require('next/router').__setUseRouter({
      pathname: `/[projectId]/datasets`,
      query: {
        projectId: PROJECT_ID,
        action: 'add-dataset-success',
        datasetId: DATASET_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: datasets })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Datasets />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // eslint-disable-next-line no-proto
    const spy = jest.spyOn(localStorage.__proto__, 'setItem')

    await act(async () => {
      component.root.findByProps({ children: 'Start Labeling' }).props.onClick()
    })

    expect(spy).toHaveBeenCalledWith(
      'leftOpeningPanelSettings',
      '{"size":400,"originSize":400,"isOpen":true,"openPanel":"assetLabeling"}',
    )

    expect(spy).toHaveBeenCalledWith(
      `AssetLabelingAdd.${PROJECT_ID}`,
      `{"datasetId":"${DATASET_ID}","label":"","scope":"TRAIN"}`,
    )

    spy.mockClear()
  })

  it('should let the user know a dataset was deleted', async () => {
    require('next/router').__setUseRouter({
      pathname: `/[projectId]/datasets`,
      query: {
        projectId: PROJECT_ID,
        action: 'delete-dataset-success',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Datasets />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
