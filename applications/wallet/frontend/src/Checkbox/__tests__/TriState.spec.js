import TestRenderer, { act } from 'react-test-renderer'

import CheckboxTriState from '../TriState'
import { VARIANTS } from '../TriStateIcon'

const noop = () => () => {}

describe('<TriState />', () => {
  it('should render properly when checked', () => {
    const component = TestRenderer.create(
      <CheckboxTriState status={VARIANTS.CHECKED} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
    act(() => {
      component.root
        .findByProps({ type: 'button' })
        .props.onClick({ preventDefault: noop })
    })
  })

  it('should render properly when unchecked', () => {
    const component = TestRenderer.create(
      <CheckboxTriState status={VARIANTS.UNCHECKED} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when partially checked', () => {
    const component = TestRenderer.create(
      <CheckboxTriState status={VARIANTS.PARTIALLY_CHECKED} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
