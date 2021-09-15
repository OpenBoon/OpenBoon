import TestRenderer, { act } from 'react-test-renderer'

import FilterLimitContent from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Slider', () => 'Slider')
jest.mock('../../Filter/Reset', () => 'FilterReset')

describe('<FilterLimitContent />', () => {
  it('should render properly', () => {
    const filter = {
      type: 'limit',
      attribute: 'utility.Search Results Limit',
      values: { maxAssets: 10_000 },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterLimitContent
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
      component.root.findByType('Slider').props.onUpdate([200])
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('Slider').props.onChange([300])
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('FilterReset').props.onReset()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should let a user input a value', () => {
    const filter = {
      type: 'limit',
      attribute: 'utility.Search Results Limit',
      values: { maxAssets: 10_000 },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterLimitContent
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
        .findByProps({ value: 10_000 })
        .props.onBlur({ target: { value: 10_000 } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()

    // cancel change when value is out of bound
    act(() => {
      component.root
        .findByProps({ value: 10_000 })
        .props.onChange({ target: { value: 12_000 } })
    })

    act(() => {
      component.root
        .findByProps({ value: 12000 })
        .props.onBlur({ target: { value: 12_000 } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()

    // update all when value is correct
    act(() => {
      component.root
        .findByProps({ value: 10_000 })
        .props.onChange({ target: { value: 10 } })
    })

    act(() => {
      component.root
        .findByProps({ value: 10 })
        .props.onKeyPress({ target: { value: 10 }, key: 'Not Enter' })
    })

    act(() => {
      component.root
        .findByProps({ value: 10 })
        .props.onKeyPress({ target: { value: 10 }, key: 'Enter' })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'limit',
          attribute: 'utility.Search Results Limit',
          values: { maxAssets: 10 },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })
})
