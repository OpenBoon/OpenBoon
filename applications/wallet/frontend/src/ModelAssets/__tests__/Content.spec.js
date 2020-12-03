import TestRenderer from 'react-test-renderer'

import assets from '../../Assets/__mocks__/assets'

import { encode } from '../../Filters/helpers'

import ModelAssetsContent from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<ModelAssets />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: assets })

    const component = TestRenderer.create(
      <ModelAssetsContent
        projectId={PROJECT_ID}
        query={encode({
          filters: [
            {
              type: 'label',
              attribute: 'labels.TestModule',
              modelId: MODEL_ID,
              values: {
                scope: 'all',
                labels: [''],
              },
            },
          ],
        })}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly without data', () => {
    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <ModelAssetsContent
        projectId={PROJECT_ID}
        query={encode({
          filters: [
            {
              type: 'label',
              attribute: 'labels.TestModule',
              modelId: MODEL_ID,
              values: {
                scope: 'all',
                labels: [''],
              },
            },
          ],
        })}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
