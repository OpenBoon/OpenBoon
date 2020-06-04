import TestRenderer, { act } from 'react-test-renderer'

import Assets from '..'

import assets from '../__mocks__/assets'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Assets />', () => {
  it('should render properly while loading', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: undefined })

    const component = TestRenderer.create(<Assets />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: assets })
    require('swr').__setPageSWRs([{ data: assets }])

    const component = TestRenderer.create(<Assets />)

    expect(component.toJSON()).toMatchSnapshot()

    // Zoom in to 4 per row
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom In' }).props.onClick()
    })

    // Zoom in to 1 per row
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom In' }).props.onClick()
    })

    // Attempt to zoom at max size
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom In' }).props.onClick()
    })

    expect(
      component.root.findByProps({ 'aria-label': 'Zoom In' }).props.isDisabled,
    ).toBe(true)
    expect(component.toJSON()).toMatchSnapshot()

    // Zoom out to 4 per row
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    // Zoom out to 8 per row
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    // Attempt to zoom out at min size
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    expect(
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.isDisabled,
    ).toBe(true)
    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render empty properly', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: { count: 0, results: [] } })
    require('swr').__setPageSWRs([{ data: { count: 0, results: [] } }])

    const component = TestRenderer.create(<Assets />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
