import TestRenderer from 'react-test-renderer'

import pdfAsset from '../../Asset/__mocks__/pdfAsset'

import MetadataPrettyRow from '../PrettyRow'

const pdfContent = pdfAsset.metadata.media.content

describe('<MetadataPrettyRow />', () => {
  it('should render documents', () => {
    require('swr').__setMockUseSWRResponse({ data: pdfAsset })

    const component = TestRenderer.create(
      <MetadataPrettyRow name="content" value={pdfContent} path="media" />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
