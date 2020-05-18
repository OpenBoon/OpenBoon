import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'

import Metrics from '..'

const PROCESSOR = asset.metadata.metrics.pipeline[0].processor

const noop = () => () => {}

describe('<Metrics />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Metrics pipeline={asset.metadata.metrics.pipeline} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})

describe('setLegend()', () => {
  describe('when user is using mouse', () => {
    it('should show legend on mouseenter', () => {
      const component = TestRenderer.create(
        <Metrics pipeline={asset.metadata.metrics.pipeline} />,
      )

      expect(component.toJSON()).toMatchSnapshot()

      act(() => {
        component.root
          .findByProps({ 'aria-label': PROCESSOR })
          .props.onMouseEnter({ preventDefault: noop })
      })

      act(() => {
        component.root
          .findByProps({ 'aria-label': PROCESSOR })
          .props.onMouseLeave({ preventDefault: noop })
      })
    })
  })

  describe('when user is using keyboard', () => {
    it('should show legend on keyPress', () => {
      const component = TestRenderer.create(
        <Metrics pipeline={asset.metadata.metrics.pipeline} />,
      )

      expect(component.toJSON()).toMatchSnapshot()

      act(() => {
        component.root
          .findByProps({ 'aria-label': PROCESSOR })
          .props.onKeyPress({ preventDefault: noop })
      })

      act(() => {
        component.root
          .findByProps({ 'aria-label': PROCESSOR })
          .props.onKeyPress({ preventDefault: noop })
      })
    })
  })
})
