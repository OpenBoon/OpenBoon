import TestRenderer, { act } from 'react-test-renderer'

import FilterExists, { noop } from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Filter/Reset', () => 'FilterReset')

describe('<FilterExists />', () => {
  it('should select "Missing"', () => {
    const filter = {
      type: 'exists',
      attribute: 'location.point',
      values: { exists: true },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterExists
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

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: btoa(
            JSON.stringify([
              {
                type: 'exists',
                attribute: 'location.point',
                values: { exists: false },
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6ImV4aXN0cyIsImF0dHJpYnV0ZSI6ImxvY2F0aW9uLnBvaW50IiwidmFsdWVzIjp7ImV4aXN0cyI6ZmFsc2V9fV0=',
    )
  })

  it('should select "Exists"', () => {
    const filter = {
      type: 'exists',
      attribute: 'location.point',
      values: { exists: false },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterExists
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

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: btoa(
            JSON.stringify([
              {
                type: 'exists',
                attribute: 'location.point',
                values: { exists: true },
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6ImV4aXN0cyIsImF0dHJpYnV0ZSI6ImxvY2F0aW9uLnBvaW50IiwidmFsdWVzIjp7ImV4aXN0cyI6dHJ1ZX19XQ==',
    )
  })

  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
