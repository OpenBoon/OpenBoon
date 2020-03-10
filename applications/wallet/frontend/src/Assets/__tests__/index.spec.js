import TestRenderer, { act } from 'react-test-renderer'

import Assets from '..'

import assets from '../__mocks__/assets'
import emptyFileAssets from '../__mocks__/emptyFileAssets'

describe('<Assets />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Assets assets={assets.results} />)

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

  it('should render without proxy image', () => {
    const component = TestRenderer.create(
      <Assets assets={emptyFileAssets.results} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
