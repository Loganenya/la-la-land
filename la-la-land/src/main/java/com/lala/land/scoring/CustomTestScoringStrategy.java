package com.lala.land.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lala.land.model.dto.question.QuestionContentDTO;
import com.lala.land.model.entity.App;
import com.lala.land.model.entity.Question;
import com.lala.land.model.entity.ScoringResult;
import com.lala.land.model.entity.UserAnswer;
import com.lala.land.model.vo.QuestionVO;
import com.lala.land.service.QuestionService;
import com.lala.land.service.ScoringResultService;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ScoringStrategyConfig(appType = 1,scoringStrategy = 0)
public class CustomTestScoringStrategy implements ScoringStrategy {
    @Resource
    private QuestionService questionService;
    @Resource
    private ScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        //1.根据id查询到题目和题目结果信息
        Long appId = app.getId();
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class)
                        .eq(Question::getAppId, appId));

        List<ScoringResult> scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId));

        //2.统计用户每个选择对应的属性个数，如1=10个，E=5个
        Map<String, Integer> optionCount = new HashMap<>();

        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();


        for (QuestionContentDTO questionContentDTO : questionContent) {
            for (String choice : choices) {
                for (QuestionContentDTO.Option option : questionContentDTO.getOptions()) {
                    if (option.getKey().equals(choice)) {
                        String result = option.getResult();

                        if (!optionCount.containsKey(result)) {
                            optionCount.put(result, 0);
                        }

                        optionCount.put(result, optionCount.get(result) + 1);
                    }
                }
            }
        }


        //3.遍历每种评分结果，计算哪个结果的得分更高。
        ScoringResult maxScoreResult = scoringResultList.get(0);
        int maxScore = 0;
        for (ScoringResult scoringResult : scoringResultList) {
            List<String> resultProp = JSONUtil.toList(scoringResult.getResultProp(), String.class);
            int score = resultProp.stream().mapToInt(prop -> optionCount.getOrDefault(prop, 0)).sum();

            if (score > maxScore) {
                maxScore = score;
                maxScoreResult = scoringResult;
            }
        }
        //4.构造返回值，填充答案对象的属性。
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoreResult.getId());
        userAnswer.setResultName(maxScoreResult.getResultName());
        userAnswer.setResultDesc(maxScoreResult.getResultDesc());
        userAnswer.setResultPicture(maxScoreResult.getResultPicture());

        return userAnswer;
    }
}
