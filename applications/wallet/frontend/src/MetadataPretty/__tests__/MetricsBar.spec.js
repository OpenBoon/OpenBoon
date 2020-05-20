import TestRenderer, { act } from 'react-test-renderer'

import MetadataPrettyMetricsBar from '../MetricsBar'

import asset from '../../Asset/__mocks__/asset'

const PIPELINE = asset.metadata.metrics.pipeline

describe('<MetadataPrettyMetricsBar />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <MetadataPrettyMetricsBar pipeline={PIPELINE} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': PIPELINE[0].processor })
        .props.onFocus()
    })

    act(() => {
      component.root
        .findByProps({ 'aria-label': PIPELINE[0].processor })
        .props.onBlur()
    })

    act(() => {
      component.root
        .findByProps({ 'aria-label': PIPELINE[0].processor })
        .props.onMouseEnter()
    })

    act(() => {
      component.root
        .findByProps({ 'aria-label': PIPELINE[0].processor })
        .props.onMouseLeave()
    })
  })
})
