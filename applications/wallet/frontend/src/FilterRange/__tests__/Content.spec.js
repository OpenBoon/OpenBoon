import TestRenderer, { act } from 'react-test-renderer'

import FilterRangeContent from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Slider', () => 'Slider')
jest.mock('../../Filter/Reset', () => 'FilterReset')

describe('<FilterRangeContent />', () => {
  it('should render with file sizes and reset', () => {
    const filter = {
      type: 'range',
      attribute: 'source.filesize',
      values: { min: 200, max: 8000 },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 584,
        results: {
          count: 580,
          min: 180,
          max: 8525.0,
          avg: 899.5689655172414,
          sum: 521750.0,
        },
      },
    })

    const component = TestRenderer.create(
      <FilterRangeContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('Slider').props.onUpdate([200, 8000])
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('Slider').props.onChange([300, 7000])
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('FilterReset').props.onReset()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not crash if min equals max', () => {
    const filter = {
      type: 'range',
      attribute: 'clip.length',
      values: {},
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 16,
        results: { count: 16, min: 1.0, max: 1.0, avg: 1.0, sum: 16.0 },
      },
    })

    const component = TestRenderer.create(
      <FilterRangeContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should let a user input a min', () => {
    const filter = {
      type: 'range',
      attribute: 'clip.length',
      values: {},
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 16,
        results: { count: 16, min: 1.0, max: 100.0, avg: 1.0, sum: 16.0 },
      },
    })

    const component = TestRenderer.create(
      <FilterRangeContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    // do nothing when value is unchanged
    act(() => {
      component.root
        .findByProps({ value: 1 })
        .props.onBlur({ target: { value: 1 } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()

    // cancel change when value is out of bound
    act(() => {
      component.root
        .findByProps({ value: 1 })
        .props.onChange({ target: { value: 150 } })
    })

    act(() => {
      component.root
        .findByProps({ value: 150 })
        .props.onBlur({ target: { value: 150 } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()

    // update all when value is correct
    act(() => {
      component.root
        .findByProps({ value: 1 })
        .props.onChange({ target: { value: 50.5555 } })
    })

    act(() => {
      component.root
        .findByProps({ value: 50.5555 })
        .props.onKeyPress({ target: { value: 50.5555 }, key: 'Not Enter' })
    })

    act(() => {
      component.root
        .findByProps({ value: 50.5555 })
        .props.onKeyPress({ target: { value: 50.5555 }, key: 'Enter' })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          assetId: '',
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          query: btoa(
            JSON.stringify([
              {
                type: 'range',
                attribute: 'clip.length',
                values: { min: 50.56, max: 100 },
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6InJhbmdlIiwiYXR0cmlidXRlIjoiY2xpcC5sZW5ndGgiLCJ2YWx1ZXMiOnsibWluIjo1MC41NiwibWF4IjoxMDB9fV0=',
    )
  })

  it('should let a user input a max', () => {
    const filter = {
      type: 'range',
      attribute: 'clip.length',
      values: {},
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 16,
        results: { count: 16, min: 1.0, max: 100.0, avg: 1.0, sum: 16.0 },
      },
    })

    const component = TestRenderer.create(
      <FilterRangeContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    // do nothing when value is unchanged
    act(() => {
      component.root
        .findByProps({ value: 100 })
        .props.onBlur({ target: { value: 100 } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()

    // cancel change when value is out of bound
    act(() => {
      component.root
        .findByProps({ value: 100 })
        .props.onChange({ target: { value: 150 } })
    })

    act(() => {
      component.root
        .findByProps({ value: 150 })
        .props.onBlur({ target: { value: 150 } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()

    // update all when value is correct
    act(() => {
      component.root
        .findByProps({ value: 100 })
        .props.onChange({ target: { value: 50.5555 } })
    })

    act(() => {
      component.root
        .findByProps({ value: 50.5555 })
        .props.onKeyPress({ target: { value: 50.5555 }, key: 'Not Enter' })
    })

    act(() => {
      component.root
        .findByProps({ value: 50.5555 })
        .props.onKeyPress({ target: { value: 50.5555 }, key: 'Enter' })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          assetId: '',
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          query: btoa(
            JSON.stringify([
              {
                type: 'range',
                attribute: 'clip.length',
                values: { min: 1, max: 50.56 },
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6InJhbmdlIiwiYXR0cmlidXRlIjoiY2xpcC5sZW5ndGgiLCJ2YWx1ZXMiOnsibWluIjoxLCJtYXgiOjUwLjU2fX1d',
    )
  })

  it('should render with no data', () => {
    const filter = {
      type: 'range',
      attribute: 'clip.length',
      values: {},
    }

    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <FilterRangeContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
