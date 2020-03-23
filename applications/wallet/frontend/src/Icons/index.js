import Head from 'next/head'
// eslint-disable-next-line import/no-extraneous-dependencies
import importAll from 'import-all.macro'

import { spacing } from '../Styles'

import PageTitle from '../PageTitle'

const icons = importAll.sync('./*.svg')

const Icons = () => {
  return (
    <div>
      <Head>
        <title>Icons</title>
      </Head>

      <PageTitle>Icons</PageTitle>

      <div css={{ display: 'flex', flexWrap: 'wrap' }}>
        {Object.entries(icons).map(([key, { default: Icon }]) => (
          <div
            key={key}
            css={{
              padding: spacing.normal,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              width: 200,
              height: 200,
            }}
          >
            <div>{key}</div>
            <div
              css={{
                height: '100%',
                padding: spacing.spacious,
                display: 'flex',
                flexDirection: 'column',
              }}
            >
              <Icon width="100%" height="100%" />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default Icons
