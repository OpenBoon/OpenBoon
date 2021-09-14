import TestRenderer, { act } from 'react-test-renderer'

import FilterLabelsExist, { noop } from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

jest.mock('../../Filter/Reset', () => 'FilterReset')

describe('<FilterLabelsExist />', () => {
  it('should select "Missing"', () => {
    const filter = {
      type: 'labelsExist',
      attribute: 'labels.pets',
      datasetId: DATASET_ID,
      values: { labelsExist: true },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterLabelsExist
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // click "Missing"
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Missing' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'labelsExist',
          attribute: 'labels.pets',
          datasetId: DATASET_ID,
          values: { exists: false },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should select "Exists"', () => {
    const filter = {
      type: 'labelsExist',
      attribute: 'labels.pets',
      datasetId: DATASET_ID,
      values: { exists: false },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterLabelsExist
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // click "Exists"
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Exists' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'labelsExist',
          attribute: 'labels.pets',
          datasetId: DATASET_ID,
          values: { exists: true },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
