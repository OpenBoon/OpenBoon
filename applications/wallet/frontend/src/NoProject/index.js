import Head from 'next/head'

import NoProjectSvg from './noProject.svg'

import { constants, colors, typography } from '../Styles'

const NoProject = () => {
  return (
    <div>
      <Head>
        <title>Zorroa</title>
      </Head>
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          height: `calc(100vh - ${constants.navbar.height}px)`,
        }}>
        <NoProjectSvg width={250} />
        <h3
          css={{
            color: colors.structure.steel,
            fontWeight: typography.weight.regular,
          }}>
          You currently have no projects.
        </h3>
      </div>
    </div>
  )
}

export default NoProject
