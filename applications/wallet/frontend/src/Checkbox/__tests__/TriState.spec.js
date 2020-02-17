import TestRenderer, { act } from 'react-test-renderer'

import CheckboxTriState from '../TriState'
import { CHECKED, UNCHECKED, PARTIALLY_CHECKED } from '../TriStateIcon'

const noop = () => () => {}

describe('<TriState />', () => {
  it('should render properly when checked', () => {
    const component = TestRenderer.create(<CheckboxTriState status={CHECKED} />)

    expect(component.toJSON()).toMatchSnapshot()
    act(() => {
      component.root
        .findByProps({ type: 'button' })
        .props.onClick({ preventDefault: noop })
    })

    act(() => {
      component.root
        .findByProps({ type: 'button' })
        .props.onKeyDown({ preventDefault: noop })
    })
  })

  it('should render properly when unchecked', () => {
    const component = TestRenderer.create(
      <CheckboxTriState status={UNCHECKED} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when partially checked', () => {
    const component = TestRenderer.create(
      <CheckboxTriState status={PARTIALLY_CHECKED} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
