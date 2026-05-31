package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.domain.A2aSkillDescriptor;

import java.util.List;

/**
 * Registry of A2A bridge skills exposed to external agents (M22).
 */
public interface A2aSkillRegistryService {

    List<A2aSkillDescriptor> listSkills();
}
