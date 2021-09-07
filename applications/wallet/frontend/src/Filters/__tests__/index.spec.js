import TestRenderer, { act } from 'react-test-renderer'

import fields from '../__mocks__/fields'

import Filters from '..'
import FilterTextSearch from '../../FilterText/Search'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

jest.mock('../MenuSection', () => 'FiltersMenuSection')
jest.mock('../CopyQuery', () => 'FiltersCopyQuery')
jest.mock('../../SearchFilter/Sort', () => 'SearchFilterSort')

describe('<Filters />', () => {
  it('should render properly', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, query: '' },
    })

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(<Filters />)

    expect(component.toJSON()).toMatchSnapshot()

    // click disabled submit button
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Search' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()

    // type search input
    act(() => {
      component.root
        .findByProps({
          placeholder: 'Type here to create a simple text filter',
        })
        .props.onChange({ value: 'Cat' })
    })

    // click enabled submit button
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Search' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'textContent',
          attribute: '',
          values: { query: 'Cat' },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should render properly and keep query params', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        assetId: ASSET_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(<Filters />)

    // type search input
    act(() => {
      component.root
        .findByProps({
          placeholder: 'Type here to create a simple text filter',
        })
        .props.onChange({ value: 'Cat' })
    })

    // click enabled submit button
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Search' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'textContent',
          attribute: '',
          values: { query: 'Cat' },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}&query=${query}`,
    )
  })

  it('should mute one filter', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        query: btoa(
          JSON.stringify([
            { type: 'textContent', attribute: '', values: { query: 'Cat' } },
          ]),
        ),
      },
    })

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(<Filters />)

    // mute Cat
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Disable Filter' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'textContent',
          attribute: '',
          values: { query: 'Cat' },
          isDisabled: true,
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should unmute one filter', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        query: btoa(
          JSON.stringify([
            {
              type: 'textContent',
              attribute: '',
              values: { query: 'Cat' },
              isDisabled: true,
            },
          ]),
        ),
      },
    })

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(<Filters />)

    // unmute Cat
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Enable Filter' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'textContent',
          attribute: '',
          values: { query: 'Cat' },
          isDisabled: false,
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should delete one filter', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        query: btoa(
          JSON.stringify([
            { type: 'textContent', attribute: '', values: { query: 'Cat' } },
            { type: 'textContent', attribute: '', values: { query: 'Dog' } },
          ]),
        ),
      },
    })

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(<Filters />)

    // delete Dog
    act(() => {
      component.root
        .findAllByType(FilterTextSearch)[1]
        .findByProps({ 'aria-label': 'Delete Filter' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'textContent',
          attribute: '',
          values: { query: 'Cat' },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should delete only filter', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        query: btoa(
          JSON.stringify([
            { type: 'textContent', attribute: '', values: { query: 'Cat' } },
          ]),
        ),
      },
    })

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(<Filters />)

    // delete Cat
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Delete Filter' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer`,
      `/${PROJECT_ID}/visualizer`,
    )
  })

  it('should clear all filters', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        query: btoa(
          JSON.stringify([
            { type: 'textContent', attribute: '', values: { query: 'Cat' } },
          ]),
        ),
      },
    })

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(<Filters />)

    // Clear All Filters
    act(() => {
      component.root
        .findByProps({ children: 'Clear All Filters' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer`,
      `/${PROJECT_ID}/visualizer`,
    )
  })

  it('should open and close the menu', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(<Filters />)

    // open the menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Add Filters' })
        .props.onClick({ preventDefault: noop })
    })

    // close the menu
    act(() => {
      component.root
        .findByProps({ children: 'Cancel' })
        .props.onClick({ preventDefault: noop })
    })
  })

  it('should add new filters', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(<Filters />)

    // open the menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Add Filters' })
        .props.onClick({ preventDefault: noop })
    })

    // Filter the fields
    act(() => {
      component.root
        .findByProps({ placeholder: 'Filter metadata fields' })
        .props.onChange({ value: 'e' })
    })

    // enable first checkbox
    act(() => {
      component.root
        .findAllByType('FiltersMenuSection')[0]
        .props.onClick({ type: 'range', attribute: 'clip.length' })(true)
    })

    // enable then disable second checkbox
    act(() => {
      component.root
        .findAllByType('FiltersMenuSection')[0]
        .props.onClick({ type: 'facet', attribute: 'clip.pile' })(true)
    })

    act(() => {
      component.root
        .findAllByType('FiltersMenuSection')[0]
        .props.onClick({ type: 'facet', attribute: 'clip.pile' })(false)
    })

    // submit
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Add Filters' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'range',
          attribute: 'clip.length',
          values: {},
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should add a new "Exists" filter', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(<Filters />)

    // open the menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Add Filters' })
        .props.onClick({ preventDefault: noop })
    })

    // enable last checkbox
    act(() => {
      component.root
        .findAllByType('FiltersMenuSection')[0]
        .props.onClick({ type: 'exists', attribute: 'location.point' })(true)
    })

    // submit
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Add Filters' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'exists',
          attribute: 'location.point',
          values: { exists: true },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should not POST the form', () => {
    const mockFn = jest.fn()

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(<Filters />)

    component.root
      .findByProps({ method: 'post' })
      .props.onSubmit({ preventDefault: mockFn })

    expect(mockFn).toHaveBeenCalled()
  })
})
