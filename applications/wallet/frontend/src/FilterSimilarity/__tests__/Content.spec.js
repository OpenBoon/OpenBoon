import TestRenderer, { act } from 'react-test-renderer'

import docAsset from '../../Asset/__mocks__/docAsset'
import imageAsset from '../../Asset/__mocks__/imageAsset'
import videoAsset from '../../Asset/__mocks__/videoAsset'

import FilterSimilarityContent from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = docAsset.id

jest.mock('../../Slider', () => 'Slider')
jest.mock('../../Filter/Reset', () => 'FilterReset')

describe('<FilterSimilarityContent />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: docAsset })

    const filter = {
      type: 'similarity',
      attribute: 'analysis.boonai-image-similarity',
      values: { ids: [ASSET_ID] },
    }

    const component = TestRenderer.create(
      <FilterSimilarityContent
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
      component.root.findByType('Slider').props.onUpdate([0.25])
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('Slider').props.onChange([0.1])
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('FilterReset').props.onReset()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render a wide asset properly', () => {
    require('swr').__setMockUseSWRResponse({ data: imageAsset })

    const filter = {
      type: 'similarity',
      attribute: 'analysis.boonai-image-similarity',
      values: { ids: [ASSET_ID] },
    }

    const component = TestRenderer.create(
      <FilterSimilarityContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render a video properly', () => {
    require('swr').__setMockUseSWRResponse({ data: videoAsset })

    const filter = {
      type: 'similarity',
      attribute: 'analysis.boonai-image-similarity',
      values: { ids: [ASSET_ID] },
    }

    const component = TestRenderer.create(
      <FilterSimilarityContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
