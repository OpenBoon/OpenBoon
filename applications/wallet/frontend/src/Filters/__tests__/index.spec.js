import TestRenderer, { act } from 'react-test-renderer'

import fields from '../__mocks__/fields'

import Filters from '..'
import FilterTextSearch from '../../FilterText/Search'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

jest.mock('../MenuSection', () => 'FiltersMenuSection')

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
          placeholder: 'Type here to create simple text filter',
        })
        .props.onChange({ target: { value: 'Cat' } })
    })

    // click enabled submit button
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Search' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          query: btoa(
            JSON.stringify([
              { type: 'textContent', attribute: '', values: { query: 'Cat' } },
            ]),
          ),
          id: '',
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6InRleHRDb250ZW50IiwiYXR0cmlidXRlIjoiIiwidmFsdWVzIjp7InF1ZXJ5IjoiQ2F0In19XQ==',
    )
  })

  it('should render properly and keep query params', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        id: ASSET_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(<Filters />)

    // type search input
    act(() => {
      component.root
        .findByProps({
          placeholder: 'Type here to create simple text filter',
        })
        .props.onChange({ target: { value: 'Cat' } })
    })

    // click enabled submit button
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Search' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          query: btoa(
            JSON.stringify([
              { type: 'textContent', attribute: '', values: { query: 'Cat' } },
            ]),
          ),
          id: 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C',
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?id=vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C&query=W3sidHlwZSI6InRleHRDb250ZW50IiwiYXR0cmlidXRlIjoiIiwidmFsdWVzIjp7InF1ZXJ5IjoiQ2F0In19XQ==',
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

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
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
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6InRleHRDb250ZW50IiwiYXR0cmlidXRlIjoiIiwidmFsdWVzIjp7InF1ZXJ5IjoiQ2F0In0sImlzRGlzYWJsZWQiOnRydWV9XQ==',
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

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          query: btoa(
            JSON.stringify([
              {
                type: 'textContent',
                attribute: '',
                values: { query: 'Cat' },
                isDisabled: false,
              },
            ]),
          ),
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6InRleHRDb250ZW50IiwiYXR0cmlidXRlIjoiIiwidmFsdWVzIjp7InF1ZXJ5IjoiQ2F0In0sImlzRGlzYWJsZWQiOmZhbHNlfV0=',
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

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          query: btoa(
            JSON.stringify([
              { type: 'textContent', attribute: '', values: { query: 'Cat' } },
            ]),
          ),
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6InRleHRDb250ZW50IiwiYXR0cmlidXRlIjoiIiwidmFsdWVzIjp7InF1ZXJ5IjoiQ2F0In19XQ==',
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
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: '',
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer',
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
        .findByProps({ 'aria-label': 'Clear All Filters' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer',
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
        .findByProps({ 'aria-label': 'Add Metadata Filters' })
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
        .findByProps({ 'aria-label': 'Add Metadata Filters' })
        .props.onClick({ preventDefault: noop })
    })

    // Filter the fields
    act(() => {
      component.root
        .findByProps({ placeholder: 'Filter metadata fields' })
        .props.onChange({ target: { value: 'e' } })
    })

    // Expand Section
    act(() => {
      component.root
        .findAllByProps({ 'aria-label': 'Expand Section' })[3]
        .props.onClick({ preventDefault: noop })
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

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: btoa(
            JSON.stringify([
              {
                type: 'range',
                attribute: 'clip.length',
                values: {},
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6InJhbmdlIiwiYXR0cmlidXRlIjoiY2xpcC5sZW5ndGgiLCJ2YWx1ZXMiOnt9fV0=',
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
        .findByProps({ 'aria-label': 'Add Metadata Filters' })
        .props.onClick({ preventDefault: noop })
    })

    // Expand Location Section
    act(() => {
      component.root
        .findAllByProps({ 'aria-label': 'Expand Section' })[6]
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
