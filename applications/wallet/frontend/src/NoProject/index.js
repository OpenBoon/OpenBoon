import Head from 'next/head'

import NoProjectSvg from '../Icons/noProject.svg'

import { colors, typography } from '../Styles'

const NoProject = () => {
  return (
    <>
      <Head>
        <title>Zorroa</title>
      </Head>

      <div
        css={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <NoProjectSvg width={250} />

        <h3
          css={{
            color: colors.structure.steel,
            fontWeight: typography.weight.regular,
          }}
        >
          You currently have no projects
        </h3>
      </div>
    </>
  )
}

export default NoProject
