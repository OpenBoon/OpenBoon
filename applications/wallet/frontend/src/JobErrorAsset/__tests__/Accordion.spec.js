import TestRenderer, { act } from 'react-test-renderer'

import JobErrorAssetAccordion from '../Accordion'
import assets from '../../Assets/__mocks__/assets'

const ASSET = assets.results[0]

describe('<JobErrorAssetAccordion />', () => {
  it('should render properly when collapsible', () => {
    const component = TestRenderer.create(
      <JobErrorAssetAccordion asset={ASSET} isCollapsible />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ role: 'button' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ role: 'button' }).props.onKeyDown()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when not collapsible', () => {
    const component = TestRenderer.create(
      <JobErrorAssetAccordion asset={ASSET} isCollapsible={false} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
