import TestRenderer, { act } from 'react-test-renderer'

import FilterDateRangeContent from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Slider', () => 'Slider')
jest.mock('../../Filter/Reset', () => 'FilterReset')
jest.mock('../../Date/helpers')

describe('<FilterDateRangeContent />', () => {
  it('should render with file sizes and reset', () => {
    const filter = {
      type: 'date',
      attribute: 'system.timeCreated',
      values: { min: '2020-05-04', max: '2020-06-10' },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          minAsString: '2020-05-04',
          maxAsString: '2020-06-10',
        },
      },
    })

    const component = TestRenderer.create(
      <FilterDateRangeContent
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
      component.root
        .findByType('Slider')
        .props.onUpdate(['2020-05-05', '2020-06-10'])
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByType('Slider')
        .props.onChange(['2020-05-05', '2020-06-09'])
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('FilterReset').props.onReset()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Use HTML input="date"
    act(() => {
      component.root
        .findByProps({ value: '2020-05-04' })
        .props.onChange({ target: { value: '2020-05-06' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ value: '2020-06-10' })
        .props.onChange({ target: { value: '2020-06-08' } })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not crash if min equals max', () => {
    const filter = {
      type: 'date',
      attribute: 'system.timeCreated',
      values: {
        min: '2020-05-04T00:00:00.000Z',
        max: '2020-05-04T00:00:00.000Z',
      },
    }

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          minAsString: '2020-05-04',
          maxAsString: '2020-06-10',
        },
      },
    })

    const component = TestRenderer.create(
      <FilterDateRangeContent
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
      type: 'date',
      attribute: 'system.timeCreated',
      values: {},
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          minAsString: '2020-05-04',
          maxAsString: '2020-06-10',
        },
      },
    })

    const component = TestRenderer.create(
      <FilterDateRangeContent
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
        .findByProps({ value: '2020-05-04' })
        .props.onBlur({ target: { value: '2020-05-04' } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()

    // do not allow values higher than max
    act(() => {
      component.root
        .findByProps({ value: '2020-05-04' })
        .props.onBlur({ target: { value: '2020-06-11' } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()

    // update all when value is correct
    act(() => {
      component.root
        .findByProps({ value: '2020-05-04' })
        .props.onBlur({ target: { value: '2020-05-06' } })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'date',
          attribute: 'system.timeCreated',
          values: {
            min: '2020-05-06T00:00:00.000Z',
            max: '2020-06-10T00:00:00.000Z',
          },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should let a user input a max', () => {
    const filter = {
      type: 'date',
      attribute: 'system.timeCreated',
      values: {},
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          minAsString: '2020-05-04',
          maxAsString: '2020-06-10',
        },
      },
    })

    const component = TestRenderer.create(
      <FilterDateRangeContent
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
        .findByProps({ value: '2020-06-10' })
        .props.onBlur({ target: { value: '2020-06-10' } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()

    // do not allow values lower than min
    act(() => {
      component.root
        .findByProps({ value: '2020-06-10' })
        .props.onBlur({ target: { value: '2020-05-03' } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()

    // update all when value is correct
    act(() => {
      component.root
        .findByProps({ value: '2020-06-10' })
        .props.onBlur({ target: { value: '2020-06-08' } })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'date',
          attribute: 'system.timeCreated',
          values: {
            min: '2020-05-04T00:00:00.000Z',
            max: '2020-06-08T00:00:00.000Z',
          },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should render with no data', () => {
    const filter = {
      attribute: 'system.timeCreated',
      type: 'date',
      values: {},
    }

    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <FilterDateRangeContent
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

  it('should handle manual navigation of invalid dates', () => {
    const filter = {
      attribute: 'system.timeCreated',
      type: 'date',
      values: {},
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          minAsString: '2020-05-04',
          maxAsString: '2020-06-10',
        },
      },
    })

    const component = TestRenderer.create(
      <FilterDateRangeContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    act(() => {
      component.root
        .findByProps({ value: '2020-05-04' })
        .props.onBlur({ target: { value: '' } })
    })

    act(() => {
      component.root
        .findByProps({ value: '2020-06-10' })
        .props.onBlur({ target: { value: '' } })
    })

    act(() => {
      component.root
        .findByProps({ value: '2020-05-04' })
        .props.onChange({ target: { value: '' } })
    })

    act(() => {
      component.root
        .findByProps({ value: '2020-06-10' })
        .props.onChange({ target: { value: '' } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()
  })
})
